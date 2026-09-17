package dev.goelo.android.rating

import dev.goelo.android.model.RecordInput

fun parseRecord(rank: Int, text: String): Result<RecordInput> = runCatching {
    require(rank in 1..9) { "段位必须为 1d 到 9d" }
    val match = Regex("^\\s*(\\d+)\\s*[-－—–]\\s*(\\d+)\\s*$").matchEntire(text)
        ?: error("战绩格式应为 W-L")
    val wins = match.groupValues[1].toIntOrNull() ?: error("胜局无效")
    val losses = match.groupValues[2].toIntOrNull() ?: error("负局无效")
    require(wins in 0..20 && losses in 0..20) { "胜负局数必须在 0 到 20 之间" }
    require(wins + losses <= 20) { "最近战绩最多 20 盘" }
    RecordInput(rank, wins, losses)
}
