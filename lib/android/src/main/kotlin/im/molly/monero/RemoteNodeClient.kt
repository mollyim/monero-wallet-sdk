package im.molly.monero

import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.*
import okhttp3.*
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.resumeWithException

internal class RemoteNodeClient(
    private val nodeSelector: RemoteNodeSelector,
    private val httpClient: OkHttpClient,
    ioDispatcher: CoroutineDispatcher,
) : IRemoteNodeClient.Stub() {

    private val logger = loggerFor<RemoteNodeClient>()

    private val requestsScope = CoroutineScope(ioDispatcher + SupervisorJob())

//    /** Disable connecting to the Monero network */
//    var offline = false

    private fun selectedNode() = nodeSelector.select()

    @CalledByNative("http_client.cc")
    override fun makeRequest(
        method: String?,
        path: String?,
        header: String?,
        body: ByteArray?,
    ): HttpResponse? {
        val selected = selectedNode()
        if (selected == null) {
            logger.w("No remote node selected")
            return null
        }
        val uri = selected.uriForPath(path ?: "")
        return try {
            execute(method, uri, header, body, selected.username, selected.password)
        } catch (ioe: IOException) {
            logger.e("HTTP: Request failed", ioe)
            return null
        } catch (e: IllegalArgumentException) {
            logger.e("HTTP: Bad request", e)
            return null
        }
    }

    @CalledByNative("http_client.cc")
    override fun cancelAll() {
        requestsScope.coroutineContext.cancelChildren()
    }

    private fun execute(
        method: String?,
        uri: Uri,
        header: String?,
        body: ByteArray?,
        username: String?,
        password: String?,
    ): HttpResponse {
        logger.d("HTTP: $method $uri, header_len=${header?.length}, body_size=${body?.size}")

        val headers = header?.parseHttpHeader()
        val contentType = headers?.get("Content-Type")?.let { value ->
            MediaType.get(value)
        }

        val request = with(Request.Builder()) {
            when (method) {
                "GET" -> {}
                "POST" -> post(RequestBody.create(contentType, body ?: ByteArray(0)))
                else -> {
                    throw IllegalArgumentException("Unsupported method")
                }
            }
            url(uri.toString())
            build()
        }

        val response = runBlocking(requestsScope.coroutineContext) {
            val call = httpClient.newCall(request)
            try {
                call.await()
            } catch (ioe: IOException) {
                if (!call.isCanceled) {
                    throw ioe
                }
                null
            }
        }

        return if (response == null) {
            HttpResponse(code = 499)
        } else if (response.isSuccessful) {
            val responseBody = requireNotNull(response.body())
            val pipe = ParcelFileDescriptor.createPipe()
            requestsScope.launch {
                pipe[1].use { writeSide ->
                    FileOutputStream(writeSide.fileDescriptor).use { outputStream ->
                        responseBody.byteStream().copyTo(outputStream)
                    }
                }
            }
            HttpResponse(
                code = response.code(),
                contentType = responseBody.contentType()?.toString(),
                body = pipe[0],
            )
        } else {
            HttpResponse(code = response.code())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun Call.await() = suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) {
                    response.close()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        })

        continuation.invokeOnCancellation { cancel() }
    }

    fun String.parseHttpHeader(): Headers =
        with(Headers.Builder()) {
            splitToSequence("\r\n")
                .filter { line -> line.isNotEmpty() }
                .forEach { line ->
                    add(line)
                }
            build()
        }
}
