package dev.goelo.android.ui

import dev.goelo.android.data.StoreSnapshot
import dev.goelo.android.data.UndoToken
import dev.goelo.android.stats.HistoryRange

enum class AppTab { GROWTH, ANALYSIS, HISTORY, SETTINGS }

data class AppUiState(
    val snapshot: StoreSnapshot? = null,
    val tab: AppTab = AppTab.GROWTH,
    val range: HistoryRange = HistoryRange.LAST20,
    val recordOpen: Boolean = false,
    val rank: Int = 7,
    val recordText: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val undo: UndoToken? = null,
    val backupOpen: Boolean = false,
    val restorePreview: dev.goelo.android.backup.PreparedRestore? = null,
    val pendingExport: Boolean = false,
    val playersOpen: Boolean = false,
    val knownOpponentMode: Boolean = false,
    val opponentId: String? = null,
)
