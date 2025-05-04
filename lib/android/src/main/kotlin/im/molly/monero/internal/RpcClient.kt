package im.molly.monero.internal

import android.net.Uri
import android.os.ParcelFileDescriptor
import im.molly.monero.BackoffPolicy
import im.molly.monero.loadbalancer.LoadBalancer
import im.molly.monero.loadbalancer.Rule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class RpcClient internal constructor(
    private val loadBalancer: LoadBalancer,
    private val loadBalancerRule: Rule,
    private val retryBackoff: BackoffPolicy,
    private val requestsScope: CoroutineScope,
    var httpClient: OkHttpClient,
) : IHttpRpcClient.Stub() {

    private val logger = loggerFor<RpcClient>()

    private val activeRequests = ConcurrentHashMap<Int, Job>()

    override fun callAsync(request: HttpRequest, callback: IHttpRequestCallback, callId: Int) {
        logger.d("[$callId] Dispatching $request")

        val requestJob = requestsScope.launch {
            runCatching {
                requestWithRetry(request, callId)
            }.onSuccess { response ->
                val status = response.code
                val responseBody = response.body
                if (responseBody == null) {
                    callback.onResponse(
                        HttpResponse(code = status, contentType = null, body = null)
                    )
                } else {
                    responseBody.use { body ->
                        val contentType = body.contentType()?.toString()
                        val pipe = ParcelFileDescriptor.createPipe()
                        pipe[0].use { readSize ->
                            pipe[1].use { writeSide ->
                                val httpResponse = HttpResponse(
                                    code = status,
                                    contentType = contentType,
                                    body = readSize,
                                )
                                callback.onResponse(httpResponse)
                                FileOutputStream(writeSide.fileDescriptor).use { out ->
                                    runCatching { body.byteStream().copyTo(out) }
                                }
                            }
                        }
                    }
                }
                // TODO: Log response times
            }.onFailure { throwable ->
                when (throwable) {
                    is CancellationException -> callback.onRequestCanceled()
                    else -> {
                        logger.e("[$callId] Failed to dispatch $request", throwable)
                        callback.onError()
                    }
                }
            }
        }.also { job ->
            val oldJob = activeRequests.put(callId, job)
            check(oldJob == null)
        }

        requestJob.invokeOnCompletion {
            activeRequests.remove(callId)
        }
    }

    override fun cancelAsync(requestId: Int) {
        activeRequests[requestId]?.cancel()
    }

    private suspend fun requestWithRetry(request: HttpRequest, callId: Int): Response {
        val headers = parseHttpHeader(request.header)
        val contentType = headers["Content-Type"]?.toMediaType()
        // TODO: Log unsupported headers
        val requestBuilder = createRequestBuilder(request, contentType)

        val attempts = mutableMapOf<Uri, Int>()

        while (true) {
            val selected = loadBalancerRule.chooseNode(loadBalancer)
            if (selected == null) {
                val errorMsg = "No remote node available"
                logger.i("[$callId] $errorMsg")
                return Response.Builder()
                    .request(requestBuilder.build())
                    .protocol(Protocol.HTTP_1_1)
                    .code(499)
                    .message(errorMsg)
                    .build()
            }

            val uri = selected.uriForPath(request.path)
            val retryCount = attempts[uri] ?: 0

            delay(retryBackoff.waitTime(retryCount))

            logger.d("[$callId] HTTP: ${request.method} $uri")

            try {
                val response =
                    httpClient.newCall(requestBuilder.url(uri.toString()).build()).await()
                // TODO: Notify loadBalancer
                if (response.isSuccessful) {
                    return response
                }
            } catch (e: IOException) {
                logger.w("[$callId] HTTP: Request failed with ${e::class.simpleName}: ${e.message}")
                // TODO: Notify loadBalancer
            } finally {
                attempts[uri] = retryCount + 1
            }
        }
    }

    private fun parseHttpHeader(header: String?): Headers {
        return with(Headers.Builder()) {
            header?.splitToSequence("\r\n")
                ?.filter { line -> line.isNotEmpty() }
                ?.forEach { line -> add(line) }
            build()
        }
    }

    private fun createRequestBuilder(
        request: HttpRequest,
        contentType: MediaType?,
    ): Request.Builder {
        return with(Request.Builder()) {
            when {
                request.method.equals("GET", ignoreCase = true) -> {}
                request.method.equals("POST", ignoreCase = true) -> {
                    val content = request.bodyBytes ?: ByteArray(0)
                    post(content.toRequestBody(contentType))
                }

                else -> throw IllegalArgumentException("Unsupported method")
            }
            url("http:${request.path}")
            // TODO: Add authentication
        }
    }

    private suspend fun Call.await() = suspendCoroutine { continuation ->
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
