package dev.goelo.android.stats

import dev.goelo.android.model.Profile
import dev.goelo.android.rating.rankBaseline
import kotlin.math.max

private fun baseline(rank: Int) = rankBaseline(rank)
private fun rankFor(elo: Double): Int = when { elo < 1100 -> 0; elo >= 2700 -> 9; else -> ((elo - 1100) / 200).toInt() + 1 }

fun goalView(profile: Profile, currentElo: Double): GoalView {
    val rank = rankFor(currentElo)
    val label = when { currentElo > 2700 -> "9 段基准以上"; currentElo >= 2700 -> "9 段参考"; rank <= 0 -> "1 段以下"; else -> "$rank 段" }
    val target = profile.targetElo ?: if (currentElo >= 2700) null else baseline(max(1, rank + 1))
    val start = profile.targetStartElo ?: if (currentElo < 1100) 0.0 else baseline(max(1, rank))
    val progress = target?.let { ((currentElo - start) / (it - start)).coerceIn(0.0, 1.0) }
    return GoalView(label, target, target?.let { max(0.0, it - currentElo) }, progress, target != null && currentElo >= target)
}
