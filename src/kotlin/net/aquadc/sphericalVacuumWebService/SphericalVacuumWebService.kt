package net.aquadc.sphericalVacuumWebService

import io.undertow.Undertow
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.BlockingHandler
import io.undertow.util.Headers
import java.time.Instant

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
        "jackson-streaming" -> JacksonStreamingParser
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
                    try {
                        val method = exchange.requestMethod

                        if (method.equalToString("get"))
                            routeByUrl(exchange)
                        else if (method.equalToString("post"))
                            routeByBody(exchange)
                        else exchange.let {
                            it.statusCode = HTTP_METHOD_NOT_SUPPORTED
                            it.responseSender.send("HTTP method $method is not supported.")
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                })
                .build()
                .start()
    }

    fun routeByUrl(exchange: HttpServerExchange) {
        val requestPath = exchange.requestPath
        val queryParameters = exchange.queryParameters
        val request = when (requestPath) {
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
        exchange.handle(request)
    }

    fun routeByBody(exchange: HttpServerExchange) {
        val contentType = exchange.requestHeaders[Headers.CONTENT_TYPE]?.first
        when (contentType) {
            "application/json" -> jsonParser.parseRequest(exchange.inputStream, exchange.requestReceiver) { exchange.handle(it) }
            else -> throw UnsupportedOperationException("Unsupported request body Content-Type: $contentType")
        }
    }

    private fun HttpServerExchange.handle(request: Request) {
        val response = when (request) {
            is JsonParseRequest -> JsonParseRequest.parse(jsonParser)
            is QuadraticEquationRequest -> request.solve(statsRepository)
            is StatRequest -> statsRepository.findStats(request)
        }

        val (serialized, contentType) = serialize(requestHeaders[Headers.ACCEPT]?.first ?: "*/*", response)

        responseHeaders.put(Headers.CONTENT_TYPE, contentType)
        responseSender.send(serialized)
    }

    fun serialize(accept: String, response: Response): Pair<String, String> = when (accept) {
        "*/*", "application/json" -> jsonParser.serializeResponse(response) to "application/json"
        else -> throw UnsupportedOperationException("Unsupported 'Accept': $accept")
    }

}
