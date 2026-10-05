package com.coffeedial.domain

data class CoffeeUsage(val name: String, val roaster: String, val count: Int)

data class HistoryAnalysis(
    val count: Int,
    val averageRating: Double?,
    val averageDose: Double?,
    val averageOutput: Double?,
    val averageSeconds: Double?,
    val averageRatio: Double?,
    val totalDose: Double,
    val ratings: Map<Int, Int>,
    val coffees: List<CoffeeUsage>
)

fun analyzeHistory(shots: List<Shot>): HistoryAnalysis {
    fun average(value: (Shot) -> Double): Double? =
        if (shots.isEmpty()) null else shots.map(value).average()
    return HistoryAnalysis(
        shots.size, average { it.rating.toDouble() }, average { it.dose },
        average { it.output }, average { it.seconds }, average { it.ratio },
        shots.sumOf {
            it.dose
        },
        (1..5).associateWith { rating -> shots.count { it.rating == rating } },
        shots.groupingBy { it.bean.name to it.bean.roaster }.eachCount().map { (bean, count) ->
            CoffeeUsage(bean.first, bean.second, count)
        }.sortedWith(
            compareByDescending<CoffeeUsage> {
                it.count
            }.thenBy { it.name }.thenBy { it.roaster }
        )
    )
}
