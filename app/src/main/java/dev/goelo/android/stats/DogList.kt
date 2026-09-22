package dev.goelo.android.stats

fun filterDogIds(ids: List<String>, query: String): List<String> {
    val needle = query.trim()
    if (needle.isEmpty()) return ids
    return ids.filter { it.contains(needle, ignoreCase = true) }
}
