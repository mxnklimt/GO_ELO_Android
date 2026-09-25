package dev.goelo.android.stats

import dev.goelo.android.model.Profile
import dev.goelo.android.rating.rankBaseline

private fun rankEntry(rank: Int) = rankBaseline(rank) - 100.0
private fun rankFor(elo: Double): Int = when {
    elo < rankEntry(1) -> 0
    elo >= rankEntry(9) -> 9
    else -> ((elo - rankEntry(1)) / 200).toInt() + 1
}

fun goalView(profile: Profile, currentElo: Double): GoalView {
    val rank = rankFor(currentElo)
    val label = when { currentElo >= rankEntry(9) + 200 -> "9 段基准以上"; rank <= 0 -> "1 段以下"; else -> "$rank 段" }
    val target = profile.targetElo ?: if (rank >= 9) null else rankEntry(rank + 1)
    val start = profile.targetStartElo ?: if (rank == 0) 0.0 else rankEntry(rank)
    val progress = target?.let { ((currentElo - start) / (it - start)).coerceIn(0.0, 1.0) }
    return GoalView(label, target, target?.let { maxOf(0.0, it - currentElo) }, progress, target != null && currentElo >= target)
}
