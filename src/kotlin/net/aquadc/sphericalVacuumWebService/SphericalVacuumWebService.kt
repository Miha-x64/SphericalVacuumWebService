package net.aquadc.sphericalVacuumWebService

import io.undertow.Undertow
import io.undertow.server.handlers.BlockingHandler
import io.undertow.util.Headers
import java.io.InputStream
import java.util.*

/**
 * Created by miha on 25.04.17
 */

private const val HTTP_METHOD_NOT_SUPPORTED = 405

fun main(args: Array<String>) {
    WebService(GsonTransformer).run()
}

class WebService(
        val jsonTransformer: JsonTransformer
) {

    fun run() {
        Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(BlockingHandler { exchange ->
                    val method = exchange.requestMethod
                    val request =
                            if (method.equalToString("get"))
                                routeByUrl(exchange.requestPath, exchange.queryParameters)
                            else if (method.equalToString("post"))
                                routeByBody(exchange.requestHeaders[Headers.CONTENT_TYPE]?.first ?: "", exchange.inputStream)
                            else exchange.let {
                                it.statusCode = HTTP_METHOD_NOT_SUPPORTED
                                it.responseSender.send("HTTP method $method is not supported.")
                                return@BlockingHandler
                            }

                    val response = handle(request)

                    val (serialized, contentType) = serialize(exchange.requestHeaders[Headers.ACCEPT].first, response)

                    exchange.responseHeaders.put(Headers.CONTENT_TYPE, contentType)
                    exchange.responseSender.send(serialized)
                })
                .build()
                .start()
    }

    fun routeByUrl(requestPath: String, queryParameters: Map<String, Deque<String>>): Request {
        return when (requestPath) {
            "/qe" -> QuadraticEquationRequest(
                    queryParameters["a"]!!.first.toDouble(),
                    queryParameters["b"]!!.first.toDouble(),
                    queryParameters["c"]!!.first.toDouble())
            else -> throw UnsupportedOperationException("can't handle address $requestPath")
        }
    }

    fun routeByBody(contentType: String, requestBody: InputStream): Request = when (contentType) {
        "application/json" -> jsonTransformer.parseRequest(requestBody)
        else -> throw UnsupportedOperationException("Unsupported request body Content-Type: $contentType")
    }

    fun handle(request: Request): Response = when (request) {
        is QuadraticEquationRequest -> QuadraticEquation.solve(request)
    }

    fun serialize(accept: String, response: Response): Pair<String, String> = when (accept) {
        "*/*", "application/json" -> jsonTransformer.serializeResponse(response) to "application/json"
        else -> throw UnsupportedOperationException("Unsupported 'Accept': $accept")
    }

}
