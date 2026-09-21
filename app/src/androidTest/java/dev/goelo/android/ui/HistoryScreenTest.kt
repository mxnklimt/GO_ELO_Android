package dev.goelo.android.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.goelo.android.model.*
import dev.goelo.android.ui.history.HistoryScreen
import dev.goelo.android.ui.theme.BlackGoldTheme
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun playerLibraryFilterLeavesOnlyKnownOpponentRows() {
        val temporary = Match("temp", 1L, MatchKind.NATIVE, 1L, "UTC", Outcome.WIN,
            RecordInput(7, 0, 0), 2300.0, 2000.0, 5.0, 2005.0, "elo-v3")
        val known = Match("known", 2L, MatchKind.NATIVE, 2L, "UTC", Outcome.LOSS,
            null, 2000.0, 2000.0, -10.0, 1990.0, "elo-pair-v1", playerId = "local",
            opponentPlayerId = "b", opponentEloAfter = 2010.0, opponentName = "Bob")
        val legacy = Match("old", 3L, MatchKind.LEGACY, null, null, Outcome.LOSS,
            null, null, 1990.0, -10.0, 1980.0, "legacy-fixed-v1")
        compose.setContent { BlackGoldTheme {
            HistoryScreen(listOf(temporary, known, legacy), 1L, false, null, { _, _, _, _ -> }, { _, _ -> })
        } }
        compose.onNodeWithText("筛选对局").performClick()
        compose.onNodeWithText("棋手库对局").performClick()
        compose.onNodeWithText("Bob", substring = true).assertIsDisplayed()
        compose.onNodeWithText("显示 1 盘 · 最近对局在前").assertIsDisplayed()
    }
}
