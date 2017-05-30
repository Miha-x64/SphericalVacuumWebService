package net.aquadc.sphericalVacuumWebService

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonObject
import com.google.gson.JsonSerializer
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Created by miha on 01.05.17
 */

interface JsonTransformer {
    fun parseRequest(requestBody: InputStream): Request
    fun serializeResponse(response: Response): String
}

object GsonTransformer : JsonTransformer {

    private val gson = GsonBuilder()
            .registerTypeAdapter(Request::class.java, JsonDeserializer<Request> { json, _, _->
                json as JsonObject
                when (json.get("method").asString) {
                    "qe" -> QuadraticEquationRequest(json.get("a").asDouble, json.get("b").asDouble, json.get("c").asDouble)
                    else -> throw UnsupportedOperationException()
                }
            })
            .registerTypeAdapter(Response::class.java, JsonSerializer<Response> { src, _, _ ->
                when (src) {
                    is QuadraticEquationSolution -> JsonObject().also {
                        it.addProperty("x1", src.x1)
                        it.addProperty("x2", src.x2)
                    }
                }
            })
            .create()

    override fun parseRequest(requestBody: InputStream): Request =
            gson.fromJson(InputStreamReader(requestBody), Request::class.java)

    override fun serializeResponse(response: Response): String =
            gson.toJson(response)

}