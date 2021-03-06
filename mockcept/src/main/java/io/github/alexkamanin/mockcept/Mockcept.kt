package io.github.alexkamanin.mockcept

import android.content.Context
import io.github.alexkamanin.mockcept.handler.PathHandler
import io.github.alexkamanin.mockcept.header.CONTENT_TYPE
import io.github.alexkamanin.mockcept.header.MEDIA_TYPE_JSON
import io.github.alexkamanin.mockcept.raw.json
import io.github.alexkamanin.mockcept.request.Method
import io.github.alexkamanin.mockcept.request.enumValueOrNull
import io.github.alexkamanin.mockcept.response.MockceptResponse
import io.github.alexkamanin.mockcept.response.StatusCode
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * @param context Application context
 * @param handlers [Sequence] of method handlers that require mocking
 * @param protocol [Protocol] from okHttp3 library
 */
class Mockcept(
    private val context: Context,
    private val handlers: List<PathHandler> = emptyList(),
    private val protocol: Protocol = Protocol.HTTP_2
) : Interceptor {

    private companion object {

        const val PARAMETERS_DELIMITER = "&"

        const val EMPTY_BODY = ""
        const val ERROR_BODY = """{ "status" : 404, "error" : "NotFound", "message" : "Request not mocked by Mockcept" }"""
    }

    private val requests = handlers.flatMap(PathHandler::requests)

    override fun intercept(chain: Interceptor.Chain): Response =
        with(chain.request()) {
            val builder = Response.Builder()
                .request(this)
                .protocol(protocol)
                .addHeader(CONTENT_TYPE, MEDIA_TYPE_JSON)

            val urlParameters = url.toUri().query?.split(PARAMETERS_DELIMITER)?.sorted() ?: emptyList()
            val foundRequest = requests
                .filter { it.path.matches(url.toUri().path) }                                        // Sorted by path
                .filter { request -> request.method == enumValueOrNull<Method>(method) }             // Sorted by request method
                .filter { request -> request.queries.isEmpty() == urlParameters.isNullOrEmpty() }    // Filter by request parameters
                .filter { request -> request.queries.zip(urlParameters).all(::matches) }             // Contains request parameters
                .shuffled()
                .firstOrNull()

            if (foundRequest == null) {
                builder.notFound()
            } else {
                builder.response(foundRequest.response)
            }

            return builder.build()
        }

    private fun matches(predicate: Pair<Regex, String>): Boolean =
        predicate.first.matches(predicate.second)

    private fun Response.Builder.response(request: MockceptResponse): Response.Builder =
        with(this) {
            val responseBody = request.body?.let(context::json) ?: EMPTY_BODY

            code(request.status.code)
            message(request.status.name)
            body(responseBody.toResponseBody())
        }

    private fun Response.Builder.notFound(): Response.Builder =
        with(this) {
            code(StatusCode.NotFound.code)
            message(StatusCode.NotFound.name)
            body(ERROR_BODY.toResponseBody())
        }
}