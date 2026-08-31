package com.github.shortiosdk

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException

/** Answers every call from memory so the suite never touches the network. */
class StubInterceptor : Interceptor {

    private val recorded = mutableListOf<Request>()
    private var responder: (Request) -> Response = {
        throw IllegalStateException("No stub configured for ${it.url}")
    }

    val requests: List<Request> get() = recorded
    val lastRequest: Request get() = recorded.last()

    val lastBody: String
        get() = lastRequest.body?.let { Buffer().also(it::writeTo).readUtf8() } ?: ""

    fun respond(code: Int, body: String = "", headers: Map<String, String> = emptyMap()) {
        responder = { request -> build(request, code, body, headers) }
    }

    fun failWith(cause: IOException) {
        responder = { throw cause }
    }

    fun client(): OkHttpClient = OkHttpClient.Builder().addInterceptor(this).build()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        recorded += request
        return responder(request)
    }

    private fun build(
        request: Request,
        code: Int,
        body: String,
        headers: Map<String, String>
    ): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(if (code in 200..299) "OK" else "Error")
        .apply { headers.forEach { (name, value) -> header(name, value) } }
        .body(body.toResponseBody("application/json".toMediaType()))
        .build()
}
