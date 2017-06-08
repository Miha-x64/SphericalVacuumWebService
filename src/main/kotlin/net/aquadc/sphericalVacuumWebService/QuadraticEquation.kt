package net.aquadc.sphericalVacuumWebService

/**
 * Created by miha on 01.05.17
 */

fun QuadraticEquationRequest.solve(statsRepository: StatsRepository): QuadraticEquationSolution {
    val (a, b, c) = this
    val d = b * b - 4 * a * c
    val solution = if (d > 0) {
        val sqr = Math.sqrt(d)
        val ta = 2 * a
        QuadraticEquationSolution(((-b - sqr) / ta), ((-b + sqr) / ta))
    } else if (d == 0.0) {
        QuadraticEquationSolution((-b / (2 * a)), Double.NaN)
    } else {
        QuadraticEquationSolution(Double.NaN, Double.NaN)
    }

    statsRepository.saveQe(a, b, c, solution.x1, solution.x2)

    return solution
}