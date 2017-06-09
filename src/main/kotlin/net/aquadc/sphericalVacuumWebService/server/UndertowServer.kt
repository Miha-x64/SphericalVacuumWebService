package net.aquadc.sphericalVacuumWebService.server

import io.undertow.Undertow
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.BlockingHandler
import io.undertow.util.Headers
import net.aquadc.sphericalVacuumWebService.BodyConsumer
import net.aquadc.sphericalVacuumWebService.Consumer
import net.aquadc.sphericalVacuumWebService.Request
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.*
import kotlin.concurrent.getOrSet

private const val HTTP_METHOD_NOT_SUPPORTED = 405

typealias MultiMap = Map<String, Deque<String>>

class UndertowServer(
        private val host: String,
        private val port: Int,
        private val parseQuery: (requestPath: String, queryParameters: MultiMap, getParameter: MultiMap.(name: String) -> String?, consumer: Consumer<Request>) -> Unit,
        private val parseBody: (contentType: String, input: InputStream, consumer: Consumer<Request>) -> Unit,
        private val handleRequestAndSerializeResponse: (Request, accept: String, buffer: ByteArray, consumer: BodyConsumer) -> Unit
) : ()->Unit {

    private val buffer = ThreadLocal<ByteArray>()

    override fun invoke() {
        Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(BlockingHandler { exchange ->
                    try {
                        val method = exchange.requestMethod

                        when {
                            method.equalToString("get") -> {
                                parseQuery(exchange.requestPath, exchange.queryParameters, { name -> this[name]?.first }) { exchange.handle(it) }

                            }
                            method.equalToString("post") -> {
                                parseBody(exchange.requestHeaders[Headers.CONTENT_TYPE]?.first!!, exchange.inputStream) { exchange.handle(it) }
                            }
                            else -> {
                                exchange.let {
                                    it.statusCode = HTTP_METHOD_NOT_SUPPORTED
                                    it.responseSender.send("HTTP method $method is not supported.")
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                })
                .build()
                .start()
    }

    private fun HttpServerExchange.handle(request: Request) {
        handleRequestAndSerializeResponse(request, requestHeaders[Headers.ACCEPT]?.first ?: "*/*", buffer.getOrSet { ByteArray(2048) }) { serialized, offset, length, contentType ->
            responseHeaders.put(Headers.CONTENT_TYPE, contentType)
            responseSender.send(ByteBuffer.wrap(serialized, offset, length))
        }
    }

}