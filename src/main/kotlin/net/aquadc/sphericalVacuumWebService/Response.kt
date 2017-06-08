package net.aquadc.sphericalVacuumWebService

/**
 * Created by miha on 01.05.17
 */

sealed class Response

data class QuadraticEquationSolution(
        val x1: Double,
        val x2: Double
) : Response()

data class StatResponse(val stats: List<QuadraticEquationStat>) : Response()

object NoResponse : Response()