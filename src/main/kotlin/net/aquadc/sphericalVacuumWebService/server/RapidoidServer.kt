package net.aquadc.sphericalVacuumWebService.server

import net.aquadc.sphericalVacuumWebService.Consumer
import net.aquadc.sphericalVacuumWebService.Request
import org.rapidoid.buffer.Buf
import org.rapidoid.data.BufRange
import org.rapidoid.data.BufRanges
import org.rapidoid.data.KeyValueRanges
import org.rapidoid.http.AbstractHttpServer
import org.rapidoid.http.HttpStatus
import org.rapidoid.http.MediaType
import org.rapidoid.net.abstracts.Channel
import org.rapidoid.net.impl.RapidoidHelper
import org.rapidoid.wrap.IntWrap
import java.io.InputStream

class RapidoidServer(
        private val host: String,
        private val port: Int,
        private val parseQuery: (requestPath: String, queryParameters: KeyValueRanges, getParameter: KeyValueRanges.(name: String) -> String?, consumer: Consumer<Request>) -> Unit,
        private val parseBody: (contentType: String, input: InputStream, consumer: Consumer<Request>) -> Unit,
        private val handleRequestAndSerializeResponse: (Request, accept: String) -> Pair<String, String>
) : AbstractHttpServer(), () -> Unit {

    override fun invoke() {
        listen(host, port)
    }

    private val GET = "GET".hashCode()
    private val POST = "POST".hashCode()
    private val Accept__ = "Accept: ".hashCode()

    override fun handle(ctx: Channel, buf: Buf, data: RapidoidHelper): HttpStatus = try {
        when (data.verb.hashCode(buf)) {
            GET -> {
                var accept = "*/*"
                val headers = data.headers
                for (i in 0 .. headers.count-1) {
                    val header = headers.get(i)
                    val start = header.start
                    if (buf.hashCode(start, start + /*Accept: */ 8) == Accept__) {
                        header.start += 8
                        accept = buf.get(header)
                        header.start = start
                        break
                    }
                }

                parseQueryString(buf, data.query, data.integers[0], data.ranges1)

                parseQuery(data.path.str(buf), data.params,
                        { data.ranges1.findVByK(buf, it, data.integers[0].value) }) { handle(ctx, it, accept) }

                HttpStatus.DONE
            }
            POST -> {
                TODO("handling POST with Rapidoid")
            }
            else -> {
                println("unsupported HTTP method: " + data.verb.str(buf))
                HttpStatus.ERROR
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        HttpStatus.ERROR
    }

    fun handle(ctx: Channel, request: Request, accept: String) {
        val (serialized, contentType) =
                handleRequestAndSerializeResponse(request, accept)

        ok(ctx, true, serialized.toByteArray(), when (contentType) {
            "application/json" -> MediaType.APPLICATION_JSON
            else -> TODO("support $contentType")
        })
    }
}

fun BufRange.hashCode(buf: Buf): Int {
    return buf.hashCode(start, last())
}

fun Buf.hashCode(first: Int, last: Int): Int {
    var hash = 0
    for (i in first .. last) {
        hash = 31 * hash + this[i]
    }
    return hash
}


private const val KV_SEPAR = '='.toByte()
private const val PAIRS_SEPAR = '&'.toByte()

internal fun parseQueryString(buf: Buf, query: BufRange, countTarget: IntWrap, queryTarget: BufRanges) {
    var idx = -1 // token index, e. g. 0=1&2=3&...
    var startIdx = query.start // beginning of current token
    var key = true // whether current token is a key
    var i = query.start
    while (i <= query.last()) {
        if (key && buf[i] == KV_SEPAR) {
            val range = queryTarget[++idx]
            range.start = startIdx
            range.length = i - startIdx
            key = false
            startIdx = i + 1
        } else if (!key && buf[i] == PAIRS_SEPAR) {
            val range = queryTarget[++idx]
            range.start = startIdx
            range.length = i - startIdx
            key = true
            startIdx = i + 1
        }
        i++
    }
    queryTarget[++idx].let {
        it.start = startIdx
        it.length = i - startIdx
    }
    countTarget.value = (idx + 1) / 2
}

internal fun BufRanges.findVByK(buf: Buf, key: String, maxIndex: Int): String? {
    var i = 0
    var last = 2 * maxIndex + 1
    while (i <= last) {
        if (key.equalTo(buf, this[i])) {
            return this[i+1].str(buf)
        }
        i++
    }
    return null
}

internal fun String.equalTo(buf: Buf, range: BufRange): Boolean {
    if (length != range.length) return false
    for (i in 0 .. length-1) {
        if (this[i].toByte() != buf[range.start + i]) return false
    }
    return true
}