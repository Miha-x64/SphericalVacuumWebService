package net.aquadc.sphericalVacuumWebService

import net.aquadc.sphericalVacuumWebService.server.RapidoidServer
import net.aquadc.sphericalVacuumWebService.server.UndertowServer
import java.io.InputStream
import java.time.Instant
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY

/**
 * Created by miha on 25.04.17
 */

typealias Consumer<T> = (T) -> Unit

fun main(args: Array<String>) {
    var host = "localhost"
    var port = 8080
    var parser = "gson-ast"
    var server = "undertow"

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--host" -> host = args[++i]
            "--port" -> port = args[++i].toInt()
            "--parser" -> parser = args[++i]
            "--server" -> server = args[++i]
            else -> throw UnsupportedOperationException("Unknown argument: ${args[i]}")
        }
        i++
    }

    val parserImpl = when (parser) {
        "gson-ast" -> GsonAstParser
        "gson-streaming" -> GsonStreamingParser
        "jackson-streaming" -> JacksonStreamingParser
        "no-op" -> NoOpParser
        else -> throw UnsupportedOperationException("unknown parser: $parser")
    }

    val bodyParser = BodyParser(parserImpl)
    val requestHandler = RequestHandler(parserImpl, InMemoryStatsRepository)

    val serverImpl = when (server) {
        "undertow" -> UndertowServer(host, port, QueryParser(), bodyParser, requestHandler)
        "rapidoid" -> RapidoidServer(host, port, QueryParser(), bodyParser, requestHandler)
        else -> throw UnsupportedOperationException("unknown server: $server")
    }

    println("Starting spherical vacuum web service" +
            "\n  on $host:$port" +
            "\n  with $parser parser" +
            "\n  on $server server")

    serverImpl()
}

class QueryParser<T> : (/*requestPath: */String, /*queryParameters: */T, /*getParameter: */T.(name: String) -> String?, /*consumer: */Consumer<Request>) -> Unit {
    override fun invoke(requestPath: String, qp: T, getParameter: T.(name: String) -> String?, consumer: Consumer<Request>) = consumer(when (requestPath) {
        "/" -> JsonParseRequest
        "/qe" -> QuadraticEquationRequest(
                qp.getParameter("a")!!.toDouble(),
                qp.getParameter("b")!!.toDouble(),
                qp.getParameter("c")!!.toDouble())
        "/stat" -> StatRequest(
                qp.getParameter("after")?.toLong()?.let { Instant.ofEpochSecond(it) },
                qp.getParameter("before")?.toLong()?.let { Instant.ofEpochSecond(it) },
                (qp.getParameter("amin")?.toDouble() ?: NEGATIVE_INFINITY)..(qp.getParameter("amax")?.toDouble() ?: POSITIVE_INFINITY),
                (qp.getParameter("bmin")?.toDouble() ?: NEGATIVE_INFINITY)..(qp.getParameter("bmax")?.toDouble() ?: POSITIVE_INFINITY),
                (qp.getParameter("cmin")?.toDouble() ?: NEGATIVE_INFINITY)..(qp.getParameter("cmax")?.toDouble() ?: POSITIVE_INFINITY),
                (qp.getParameter("x1min")?.toDouble() ?: NEGATIVE_INFINITY)..(qp.getParameter("x1max")?.toDouble() ?: POSITIVE_INFINITY),
                (qp.getParameter("x2min")?.toDouble() ?: NEGATIVE_INFINITY)..(qp.getParameter("x2max")?.toDouble() ?: POSITIVE_INFINITY)
        )
        else -> throw UnsupportedOperationException("can't handle address $requestPath")
    })
}

class BodyParser(val jsonParser: JsonParser) : (String, InputStream, (Request) -> Unit) -> Unit {
    override fun invoke(contentType: String, input: InputStream, consumer: (Request) -> Unit) = when (contentType) {
        "application/json" -> jsonParser.parseRequest(input, consumer)
        else -> throw UnsupportedOperationException("Unsupported request body Content-Type: $contentType")
    }
}

class RequestHandler(
        private val jsonParser: JsonParser,
        private val statsRepository: StatsRepository
) : (Request, /*accept: */String) -> Pair<String, String> {
    override fun invoke(request: Request, accept: String): Pair<String, String> {
        val response = when (request) {
            is JsonParseRequest -> JsonParseRequest.parse(jsonParser)
            is QuadraticEquationRequest -> request.solve(statsRepository)
            is StatRequest -> statsRepository.findStats(request)
        }
        return when (accept) {
            "*/*", "application/json" -> jsonParser.serializeResponse(response) to "application/json"
            else -> throw UnsupportedOperationException("Unsupported 'Accept': $accept")
        }
    }
}
