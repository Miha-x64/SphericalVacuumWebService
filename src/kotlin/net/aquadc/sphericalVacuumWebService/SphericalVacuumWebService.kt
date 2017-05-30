package net.aquadc.sphericalVacuumWebService

import io.undertow.Undertow
import io.undertow.server.handlers.BlockingHandler
import io.undertow.util.Headers
import java.io.InputStream
import java.time.Instant
import java.util.*

/**
 * Created by miha on 25.04.17
 */

private const val HTTP_METHOD_NOT_SUPPORTED = 405

fun main(args: Array<String>) {
    var parser = "gson-ast"
    var host = "localhost"
    var port = 8080

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--parser" -> parser = args[++i]
            "--host" -> host = args[++i]
            "--port" -> port = args[++i].toInt()
            else -> throw UnsupportedOperationException("Unknown argument: ${args[i]}")
        }
        i++
    }

    val parserImpl = when (parser) {
        "gson-ast" -> GsonAstParser
        "gson-streaming" -> GsonStreamingParser
        else -> throw UnsupportedOperationException("unknown parser: $parser")
    }

    println("Starting spherical vacuum web service on $host:$port with $parser parser...")

    WebService(parserImpl, host, port, InMemoryStatsRepository).run()
}

class WebService(
        private val jsonParser: JsonParser,
        private val host: String,
        private val port: Int,
        private val statsRepository: StatsRepository
) {

    fun run() {
        Undertow.builder()
                .addHttpListener(port, host)
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

                    val (serialized, contentType) = serialize(exchange.requestHeaders[Headers.ACCEPT]?.first ?: "*/*", response)

                    exchange.responseHeaders.put(Headers.CONTENT_TYPE, contentType)
                    exchange.responseSender.send(serialized)
                })
                .build()
                .start()
    }

    fun routeByUrl(requestPath: String, queryParameters: Map<String, Deque<String>>): Request {
        return when (requestPath) {
            "/" -> JsonParseRequest
            "/qe" -> QuadraticEquationRequest(
                    queryParameters["a"]!!.first.toDouble(),
                    queryParameters["b"]!!.first.toDouble(),
                    queryParameters["c"]!!.first.toDouble())
            "/stat" -> StatRequest(
                    queryParameters["after"]?.first?.toLong()?.let { Instant.ofEpochSecond(it) },
                    queryParameters["before"]?.first?.toLong()?.let { Instant.ofEpochSecond(it) },
                    (queryParameters["amin"]?.first?.toDouble() ?: Double.NEGATIVE_INFINITY) .. (queryParameters["amax"]?.first?.toDouble() ?: Double.POSITIVE_INFINITY),
                    (queryParameters["bmin"]?.first?.toDouble() ?: Double.NEGATIVE_INFINITY) .. (queryParameters["bmax"]?.first?.toDouble() ?: Double.POSITIVE_INFINITY),
                    (queryParameters["cmin"]?.first?.toDouble() ?: Double.NEGATIVE_INFINITY) .. (queryParameters["cmax"]?.first?.toDouble() ?: Double.POSITIVE_INFINITY),
                    (queryParameters["x1min"]?.first?.toDouble() ?: Double.NEGATIVE_INFINITY) .. (queryParameters["x1max"]?.first?.toDouble() ?: Double.POSITIVE_INFINITY),
                    (queryParameters["x2min"]?.first?.toDouble() ?: Double.NEGATIVE_INFINITY) .. (queryParameters["x2max"]?.first?.toDouble() ?: Double.POSITIVE_INFINITY)
            )
            else -> throw UnsupportedOperationException("can't handle address $requestPath")
        }
    }

    fun routeByBody(contentType: String, requestBody: InputStream): Request = when (contentType) {
        "application/json" -> jsonParser.parseRequest(requestBody)
        else -> throw UnsupportedOperationException("Unsupported request body Content-Type: $contentType")
    }

    fun handle(request: Request): Response = when (request) {
        is JsonParseRequest -> JsonParseRequest.parse(jsonParser)
        is QuadraticEquationRequest -> request.solve(statsRepository)
        is StatRequest -> statsRepository.findStats(request)
    }

    fun serialize(accept: String, response: Response): Pair<String, String> = when (accept) {
        "*/*", "application/json" -> jsonParser.serializeResponse(response) to "application/json"
        else -> throw UnsupportedOperationException("Unsupported 'Accept': $accept")
    }

}
