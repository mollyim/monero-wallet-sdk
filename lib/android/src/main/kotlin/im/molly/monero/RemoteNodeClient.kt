package im.molly.monero

import android.net.Uri
import android.os.ParcelFileDescriptor
import im.molly.monero.loadbalancer.LoadBalancer
import im.molly.monero.loadbalancer.Rule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// TODO: Hide IRemoteNodeClient methods and rename to HttpRpcClient
class RemoteNodeClient private constructor(
    val network: MoneroNetwork,
    private val loadBalancer: LoadBalancer,
    private val loadBalancerRule: Rule,
    var httpClient: OkHttpClient,
    private val retryBackoff: BackoffPolicy,
    private val requestsScope: CoroutineScope,
) : IRemoteNodeClient.Stub(), AutoCloseable {

    companion object {
        /**
         * Constructs a [RemoteNodeClient] to connect to the Monero [network].
         */
        fun forNetwork(
            network: MoneroNetwork,
            remoteNodes: Flow<List<RemoteNode>>,
            loadBalancerRule: Rule,
            httpClient: OkHttpClient,
            retryBackoff: BackoffPolicy = ExponentialBackoff.Default,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): RemoteNodeClient {
            val scope = CoroutineScope(ioDispatcher + SupervisorJob())
            return RemoteNodeClient(
                network,
                LoadBalancer(remoteNodes, scope),
                loadBalancerRule,
                httpClient,
                retryBackoff,
                scope
            )
        }
    }

    private val logger = loggerFor<RemoteNodeClient>()

    private val requestList = ConcurrentHashMap<Int, Job>()

    override fun requestAsync(
        requestId: Int,
        method: String,
        path: String,
        header: String?,
        body: ByteArray?,
        callback: IHttpRequestCallback?,
    ) {
        logger.d("HTTP: $method $path, header_len=${header?.length}, body_size=${body?.size}")

        val requestJob = requestsScope.launch {
            runCatching {
                requestWithRetry(method, path, header, body)
            }.onSuccess { response ->
                val status = response.code
                val responseBody = response.body
                if (responseBody == null) {
                    callback?.onResponse(status, null, null)
                } else {
                    responseBody.use { body ->
                        val contentType = body.contentType()?.toString()
                        val pipe = ParcelFileDescriptor.createPipe()
                        pipe[1].use { writeSide ->
                            callback?.onResponse(status, contentType, pipe[0])
                            FileOutputStream(writeSide.fileDescriptor).use { out ->
                                runCatching { body.byteStream().copyTo(out) }
                            }
                        }
                    }
                }
                // TODO: Log response times
            }.onFailure { throwable ->
                logger.e("HTTP: Request failed", throwable)
                callback?.onFailure()
            }
        }.also {
            requestList[requestId] = it
        }

        requestJob.invokeOnCompletion {
            requestList.remove(requestId)
        }
    }

    override fun cancelAsync(requestId: Int) {
        requestList[requestId]?.cancel()
    }

    override fun close() {
        requestsScope.cancel()
    }

    private suspend fun requestWithRetry(
        method: String,
        path: String,
        header: String?,
        body: ByteArray?,
    ): Response {
        val attempts = mutableMapOf<Uri, Int>()

        while (true) {
            val selected = loadBalancerRule.chooseNode(loadBalancer)
            if (selected == null) {
                logger.i("No remote node available")

                return Response.Builder().code(499).build()
            }

            val uri = selected.uriForPath(path)
            val retryCount = attempts[uri] ?: 0

            delay(retryBackoff.waitTime(retryCount))

            val response = try {
                executeCall(
                    method = method,
                    uri = uri,
                    username = selected.username,
                    password = selected.password,
                    header = header,
                    body = body,
                )
            } catch (e: IOException) {
                logger.e("HTTP: Request failed", e)
                // TODO: Notify loadBalancer
                continue
            } finally {
                attempts[uri] = retryCount + 1
            }

            if (response.isSuccessful) {
                // TODO: Notify loadBalancer
                return response
            }
        }
    }

    private suspend fun executeCall(
        method: String?,
        uri: Uri,
        username: String?,
        password: String?,
        header: String?,
        body: ByteArray?,
    ): Response {
        val headers = parseHttpHeader(header)
        val contentType = headers["Content-Type"]?.toMediaType()
        // TODO: Log unsupported headers
        val request = with(Request.Builder()) {
            when {
                method.equals("GET", ignoreCase = true) -> {}
                method.equals("POST", ignoreCase = true) -> {
                    val content = body ?: ByteArray(0)
                    post(content.toRequestBody(contentType))
                }
                else -> throw IllegalArgumentException("Unsupported method")
            }
            // TODO: Add authentication
            url(uri.toString())
            build()
        }
        return httpClient.newCall(request).await()
    }

    private fun parseHttpHeader(header: String?): Headers =
        with(Headers.Builder()) {
            header?.splitToSequence("\r\n")
                ?.filter { line -> line.isNotEmpty() }
                ?.forEach { line -> add(line) }
            build()
        }

    private suspend fun Call.await() =
        suspendCoroutine { continuation ->
            enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }
            })
        }

//    private val Response.roundTripMillis: Long
//        get() = sentRequestAtMillis() - receivedResponseAtMillis()

}

@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun IRemoteNodeClient.request(request: HttpRequest): HttpResponse? =
    suspendCancellableCoroutine { continuation ->
        val requestId = request.hashCode()
        val callback = object : IHttpRequestCallback.Stub() {
            override fun onResponse(
                code: Int,
                contentType: String?,
                body: ParcelFileDescriptor?,
            ) {
                continuation.resume(HttpResponse(code, contentType, body)) {
                    body?.close()
                }
            }

            override fun onFailure() {
                continuation.resume(null) {}
            }
        }
        with(request) {
            requestAsync(requestId, method, path, header, bodyBytes, callback)
        }
        continuation.invokeOnCancellation {
            cancelAsync(requestId)
        }
    }
