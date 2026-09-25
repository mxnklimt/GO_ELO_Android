package dev.goelo.android.ui.components

import dev.goelo.android.rating.rankBaseline
import dev.goelo.android.model.Outcome
import dev.goelo.android.stats.SeriesPoint
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

data class RankEntry(val rank: Int, val elo: Double)
data class ChartScale(val lower: Double, val upper: Double, val ticks: List<Double>, val rankEntries: List<RankEntry>)
enum class ChartMarkerShape { CIRCLE, DIAMOND }

fun chartMarkerShape(outcome: Outcome?): ChartMarkerShape =
    if (outcome == Outcome.LOSS) ChartMarkerShape.DIAMOND else ChartMarkerShape.CIRCLE

fun chartSelectionStep(current: Int?, pointCount: Int, direction: Int): Int? {
    if (pointCount <= 0) return null
    return ((current ?: pointCount - 1) + direction).coerceIn(0, pointCount - 1)
}

fun chartPointDetail(point: SeriesPoint): String = if (point.matchId == null) {
    "区间起点 · ${eloText(point.elo)} ELO"
} else {
    val outcome = if (point.outcome == Outcome.WIN) "胜" else "负"
    "第 ${point.order} 盘 · $outcome · ${eloText(point.elo)} ELO · ${deltaText(point.delta ?: 0.0)}"
}

fun chartScale(points: List<SeriesPoint>): ChartScale {
    val min = points.minOfOrNull { it.elo } ?: 0.0
    val max = points.maxOfOrNull { it.elo } ?: 0.0
    val padding = max(5.0, (max - min) * 0.12)
    val lower = min - padding
    val upper = max + padding
    val roughStep = (upper - lower) / 4
    val magnitude = 10.0.pow(floor(log10(roughStep)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).first { it * magnitude >= roughStep } * magnitude
    val ticks = generateSequence(ceil(lower / step) * step) { it + step }
        .takeWhile { it <= upper + 0.000001 }.toList()
    val entries = (1..9).map { RankEntry(it, rankBaseline(it) - 100.0) }
        .filter { it.elo in lower..upper }
    return ChartScale(lower, upper, ticks, entries)
}

fun visibleMarkerIndices(pointCount: Int): List<Int> {
    if (pointCount <= 1) return emptyList()
    if (pointCount <= 21) return (1 until pointCount).toList()
    val stride = ceil((pointCount - 1) / 18.0).toInt()
    return ((1 until pointCount).filter { it % stride == 0 } + (pointCount - 1)).distinct()
}
