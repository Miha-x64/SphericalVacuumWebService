package net.aquadc.sphericalVacuumWebService

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringWriter
import java.time.Instant

/**
 * Created by miha on 01.05.17
 */

interface JsonParser {
    fun parseRequest(inputStream: InputStream, consumer: (Request)->Unit)
    fun serializeResponse(response: Response): String
}

object GsonAstParser : JsonParser {

    private val gson = GsonBuilder()
            .registerTypeAdapter(Request::class.java, JsonDeserializer<Request> { json, _, _->
                json as JsonObject
                when (json.get("method").asString) {
                    "qe" -> QuadraticEquationRequest(json.get("a").asDouble, json.get("b").asDouble, json.get("c").asDouble)
                    "stat" -> StatRequest(
                            after = json.get("after")?.asInstant,
                            before = json.get("before")?.asInstant,
                            a = json.get("a")?.asDoubleRange,
                            b = json.get("b")?.asDoubleRange,
                            c = json.get("c")?.asDoubleRange,
                            x1 = json.get("x1")?.asDoubleRange,
                            x2 = json.get("x2")?.asDoubleRange
                    )
                    else -> throw UnsupportedOperationException()
                }
            })
            .registerTypeAdapter(Response::class.java, JsonSerializer<Response> { src, _, _ ->
                when (src) {
                    is QuadraticEquationSolution -> JsonObject().also {
                        it.add("x1", JsonPrimitive(src.x1))
                        it.add("x2", JsonPrimitive(src.x2))
                    }
                    is StatResponse -> JsonArray().also { ja ->
                        src.stats.forEach { stat -> ja.add(JsonObject().also {
                            it.add("a", JsonPrimitive(stat.a))
                            it.add("b", JsonPrimitive(stat.b))
                            it.add("c", JsonPrimitive(stat.c))
                            it.add("x1", JsonPrimitive(stat.x1))
                            it.add("x2", JsonPrimitive(stat.x2))
                            it.add("date", JsonPrimitive(stat.date.epochSecond))
                        }) }
                    }
                    is NoResponse -> JsonNull.INSTANCE
                }
            })
            .serializeSpecialFloatingPointValues()
            .create()

    override fun parseRequest(inputStream: InputStream, consumer: (Request)->Unit) {
        consumer(gson.fromJson(InputStreamReader(inputStream), Request::class.java))
    }

    override fun serializeResponse(response: Response): String =
            gson.toJson(response, Response::class.java)

    private val JsonElement.asInstant get() = Instant.ofEpochSecond(asLong)
    private val JsonElement.asDoubleRange get() = asJsonObject.let {
        (it.get("min")?.asDouble ?: Double.NEGATIVE_INFINITY) .. (it.get("max")?.asDouble ?: Double.POSITIVE_INFINITY)
    }
}

object GsonStreamingParser : JsonParser {

    private val gson = GsonBuilder()
            .registerTypeAdapter(Request::class.java, object : TypeAdapter<Request>() {
                override fun read(reader: JsonReader): Request {
                    reader.beginObject()

                    if (reader.nextName() != "method") {
                        throw IllegalArgumentException("For streaming, method must be specified first")
                    }
                    val response = when (reader.nextString()) {
                        "qe" -> TODO("qe parsing") // todo
                        "stat" -> {
                            var after = Long.MIN_VALUE
                            var before = Long.MIN_VALUE
                            var a: ClosedRange<Double>? = null
                            var b: ClosedRange<Double>? = null
                            var c: ClosedRange<Double>? = null
                            var x1: ClosedRange<Double>? = null
                            var x2: ClosedRange<Double>? = null

                            while (reader.hasNext()) {
                                val name = reader.nextName()
                                when (name) {
                                    "after" -> { after = reader.nextLong() }
                                    "before" -> { before = reader.nextLong() }
                                    "a" -> a = reader.nextDoubleRange()
                                    "b" -> b = reader.nextDoubleRange()
                                    "c" -> c = reader.nextDoubleRange()
                                    "x1" -> x1 = reader.nextDoubleRange()
                                    "x2" -> x2 = reader.nextDoubleRange()
                                    else -> throw UnsupportedOperationException("unknown key in 'stat': $name")
                                }
                            }

                            StatRequest(
                                    if (after == Long.MIN_VALUE) null else Instant.ofEpochSecond(after),
                                    if (before == Long.MIN_VALUE) null else Instant.ofEpochSecond(before),
                                    a, b, c, x1, x2)
                        }
                        else -> throw UnsupportedOperationException()
                    }

                    reader.endObject()

                    return response
                }

                override fun write(out: JsonWriter?, value: Request?) = throw UnsupportedOperationException()
            })
            .registerTypeAdapter(Response::class.java, object : TypeAdapter<Response>() {
                override fun read(`in`: JsonReader?): Response = throw UnsupportedOperationException()

                override fun write(out: JsonWriter, value: Response) {
                    val exhaustive = when (value) {
                        is QuadraticEquationSolution -> {
                            out.beginObject()
                            out.name("x1"); out.value(java.lang.Double.valueOf(value.x1) as Number)
                            out.name("x2"); out.value(java.lang.Double.valueOf(value.x2) as Number)
                            out.endObject()
                        }
                        is StatResponse -> {
                            out.beginArray()
                            value.stats.forEach {
                                out.beginObject()
                                out.name("a"); out.value(java.lang.Double.valueOf(it.a) as Number)
                                out.name("b"); out.value(java.lang.Double.valueOf(it.b) as Number)
                                out.name("c"); out.value(java.lang.Double.valueOf(it.c) as Number)
                                out.name("x1"); out.value(java.lang.Double.valueOf(it.x1) as Number)
                                out.name("x1"); out.value(java.lang.Double.valueOf(it.x2) as Number)
                                out.name("date"); out.value(it.date.epochSecond)
                                out.endObject()
                            }
                            out.endArray()
                        }
                        is NoResponse -> out.nullValue()
                    }
                }
            })
            .create()

    override fun parseRequest(inputStream: InputStream, consumer: (Request)->Unit) {
        consumer(gson.fromJson(InputStreamReader(inputStream), Request::class.java))
    }

    override fun serializeResponse(response: Response): String =
            gson.toJson(response, Response::class.java)

    private fun JsonReader.nextDoubleRange(): ClosedRange<Double> {
        beginObject()
        var min = Double.NEGATIVE_INFINITY
        var max = Double.POSITIVE_INFINITY
        while (hasNext()) {
            val name = nextName()
            when (name) {
                "min" -> min = nextDouble()
                "max" -> max = nextDouble()
                else -> throw UnsupportedOperationException("unknown key in range: $name")
            }
        }
        endObject()
        return min..max
    }
}

/*object DslJsonParser : JsonParser {

    val dslJson = DslJson<Any>()

    override fun parseRequest(requestBody: InputStream): Request {
        return dslJson.deserialize(Request::class.java, requestBody, kotlin.ByteArray(1024))
    }

    override fun serializeResponse(response: Response): String {
        val writer = dslJson.newWriter()
        val os = ByteArrayOutputStream()
        dslJson.serialize(response, os)
        return os.toString("UTF-8")
    }

}*/

object JacksonStreamingParser : JsonParser {

    val factory = JsonFactory().also { it.enable(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS) }

    override fun parseRequest(inputStream: InputStream, consumer: (Request) -> Unit) {
        val parser = factory.createParser(inputStream)
        check(parser.nextToken() == JsonToken.START_OBJECT)
        check(parser.nextFieldName() == "method", { "first name in JSON is ${parser.currentName}, but for streaming it must be 'method'" })
        val method = parser.nextTextValue()
        consumer(when (method) {
            "qe" -> TODO("qe parsing")
            "stat" -> {
                var after = Long.MIN_VALUE
                var before = Long.MIN_VALUE
                var a: ClosedRange<Double>? = null
                var b: ClosedRange<Double>? = null
                var c: ClosedRange<Double>? = null
                var x1: ClosedRange<Double>? = null
                var x2: ClosedRange<Double>? = null

                while (true) {
                    val name = parser.nextFieldName() ?: break
                    when (name) {
                        "after" -> { check(parser.nextValue() == JsonToken.VALUE_NUMBER_INT); after = parser.longValue }
                        "before" -> { check(parser.nextValue() == JsonToken.VALUE_NUMBER_INT); before = parser.longValue }
                        "a" -> a = parser.nextDoubleRangeValue()
                        "b" -> b = parser.nextDoubleRangeValue()
                        "c" -> c = parser.nextDoubleRangeValue()
                        "x1" -> x1 = parser.nextDoubleRangeValue()
                        "x2" -> x2 = parser.nextDoubleRangeValue()
                        else -> throw UnsupportedOperationException("unknown key in 'stat': $name")
                    }
                }

                StatRequest(
                        if (after == Long.MIN_VALUE) null else Instant.ofEpochSecond(after),
                        if (before == Long.MIN_VALUE) null else Instant.ofEpochSecond(before),
                        a, b, c, x1, x2)
            }
            else -> throw UnsupportedOperationException()
        })
    }

    override fun serializeResponse(response: Response): String {
        val out = StringWriter()
        val gen = factory.createGenerator(out)
        val ex = when (response) {
            is QuadraticEquationSolution -> {
                gen.writeStartObject()
                gen.writeNumberField("x1", response.x1)
                gen.writeNumberField("x2", response.x2)
                gen.writeEndObject()
            }
            is StatResponse -> {
                gen.writeStartArray()

                response.stats.forEach {
                    gen.writeStartObject()
                    gen.writeNumberField("a", it.a)
                    gen.writeNumberField("b", it.b)
                    gen.writeNumberField("c", it.c)
                    gen.writeNumberField("x1", it.x1)
                    gen.writeNumberField("x2", it.x2)
                    gen.writeNumberField("date", it.date.epochSecond)
                    gen.writeEndObject()
                }

                gen.writeEndArray()
            }
            NoResponse -> gen.writeNull()
        }
        gen.flush()
        return out.toString()
    }

    private fun com.fasterxml.jackson.core.JsonParser.nextDoubleRangeValue(): ClosedRange<Double> {
        check(nextToken() == JsonToken.START_OBJECT)
        var min = Double.NEGATIVE_INFINITY
        var max = Double.POSITIVE_INFINITY
        while (true) {
            val name = nextFieldName() ?: break
            val curVal = nextValue()
            check(curVal == JsonToken.VALUE_NUMBER_FLOAT || curVal == JsonToken.VALUE_NUMBER_INT, { "current value expected to be a number, got $curVal" })
            when (name) {
                "min" -> min = doubleValue
                "max" -> max = doubleValue
                else -> throw UnsupportedOperationException("unknown key in range: $name")
            }
        }
        check(currentToken == JsonToken.END_OBJECT, { "expected end object, got $currentToken" })
        return min..max
    }

}

object NoOpParser : JsonParser {

    override fun parseRequest(inputStream: InputStream, consumer: (Request) -> Unit) {
        consumer(JsonParseRequest)
    }

    override fun serializeResponse(response: Response): String {
        return "null"
    }

}

/*object CustomParser : JsonParser {

    override fun parseRequest(inputStream: InputStream, receiver: Receiver, consumer: (Request) -> Unit) {
        receiver.receivePartialBytes { _, message, last ->

        }
    }

    override fun serializeResponse(response: Response): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    priva

    private enum class JsonToken {
        BeginObject, EndObject
    }
}*/
