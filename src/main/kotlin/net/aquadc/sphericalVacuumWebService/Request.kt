package net.aquadc.sphericalVacuumWebService

import java.io.ByteArrayInputStream
import java.time.Instant

/**
 * Created by miha on 01.05.17
 */

sealed class Request

data class QuadraticEquationRequest(val a: Double, val b: Double, val c: Double) : Request()

data class StatRequest(val after: Instant?, val before: Instant?,
                       val a: ClosedRange<Double>?, val b: ClosedRange<Double>?, val c: ClosedRange<Double>?,
                       val x1: ClosedRange<Double>?, val x2: ClosedRange<Double>?) : Request()

object JsonParseRequest : Request() {

    val req = """{
	"method": "stat",
	"after": 0,
	"before": 100500200700,
	"a": {
		"min": -Infinity,
		"max": Infinity
	},
	"b": {
		"min": -200700,
		"max": 100500
	},
	"c": {
		"min": 1234,
		"max": 9999
	},
	"x1": {
		"min": -10,
		"max": 20
	},
	"x2": {
		"min": -100,
		"max": NaN
	}
}"""

    fun parse(jsonParser: JsonParser): NoResponse {

        jsonParser.parseRequest(ByteArrayInputStream(req.toByteArray()), { })

        return NoResponse
    }
}