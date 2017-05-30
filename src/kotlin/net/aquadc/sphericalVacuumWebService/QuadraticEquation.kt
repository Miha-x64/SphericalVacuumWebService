package net.aquadc.sphericalVacuumWebService

/**
 * Created by miha on 01.05.17
 */
object QuadraticEquation {

    fun solve(condition: QuadraticEquationRequest): QuadraticEquationSolution {
        val (a, b, c) = condition
        val d = b * b - 4 * a * c
        return if (d > 0) {
            val sqr = Math.sqrt(d)
            val ta = 2 * a
            QuadraticEquationSolution(((-b - sqr) / ta), ((-b + sqr) / ta))
        } else if (d == 0.0) {
            QuadraticEquationSolution((-b / (2 * a)), Double.NaN)
        } else {
            QuadraticEquationSolution(Double.NaN, Double.NaN)
        }
    }

}