package net.aquadc.sphericalVacuumWebService

import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

interface StatsRepository {
    fun saveQe(a: Double, b: Double, c: Double, x1: Double, x2: Double)
    fun findStats(request: StatRequest): StatResponse
}

data class QuadraticEquationStat(
        val date: Instant, val a: Double, val b: Double, val c: Double, val x1: Double, val x2: Double)





object InMemoryStatsRepository : StatsRepository {

    private val qeStat = CopyOnWriteArrayList<QuadraticEquationStat>()

    override fun saveQe(a: Double, b: Double, c: Double, x1: Double, x2: Double) {
        qeStat += QuadraticEquationStat(Instant.now(), a, b, c, x1, x2)
    }

    override fun findStats(request: StatRequest): StatResponse {
        val (after, before, a, b, c, x1, x2) = request
        var ans: List<QuadraticEquationStat> = qeStat
        if (after != null) ans = ans.filter { it.date > after }
        if (before != null) ans = ans.filter { it.date < before }
        if (a != null) ans = ans.filter { it.a in a }
        if (b != null) ans = ans.filter { it.b in b }
        if (c != null) ans = ans.filter { it.c in c }
        if (x1 != null) ans = ans.filter { it.x1.isNaN() || it.x1 in x1 }
        if (x2 != null) ans = ans.filter { it.x2.isNaN() || it.x2 in x2 }
        return StatResponse(ans)
    }

}