package dev.goelo.android.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.goelo.android.model.*
import dev.goelo.android.stats.HistoryRange
import dev.goelo.android.ui.analysis.AnalysisScreen
import dev.goelo.android.ui.theme.BlackGoldTheme
import org.junit.Rule
import org.junit.Test

class AnalysisInsightsTest {
    @get:Rule val compose = createComposeRule()

    private fun ledger(withOpponent: Boolean): AppState {
        val known = Match(
            "ab", 1L, MatchKind.NATIVE, 1L, "UTC", Outcome.WIN, null,
            2000.0, 2000.0, 10.0, 2010.0, "elo-pair-v1",
            playerId = "local", opponentPlayerId = "b", opponentEloAfter = 1990.0,
        )
        return AppState(
            Profile(id = "local", name = "甲", initialElo = 2000.0),
            if (withOpponent) listOf(known) else emptyList(),
            if (withOpponent) listOf(Profile(id = "b", name = "Bob", initialElo = 2000.0)) else emptyList(),
        )
    }

    @Test fun analysisShowsPlayerPredictionAndHeadToHeadSummary() {
        val state = ledger(true)
        compose.setContent { BlackGoldTheme {
            AnalysisScreen(ledger = state, personal = state.forPlayer("local"), range = HistoryRange.ALL) {}
        } }
        compose.onNodeWithText("胜率预测").assertIsDisplayed()
        compose.onNodeWithText("棋手库").performClick()
        compose.onNodeWithTag("prediction-opponent-b").performClick()
        compose.onNodeWithText("你的胜率", substring = true).assertIsDisplayed()
        compose.onNodeWithText("棋手对局").assertIsDisplayed()
        compose.onNodeWithText("Bob · 1胜 0负", substring = true).assertIsDisplayed()
    }

    @Test fun analysisShowsEmptyPlayerLibraryState() {
        val state = ledger(false)
        compose.setContent { BlackGoldTheme {
            AnalysisScreen(ledger = state, personal = state.forPlayer("local"), range = HistoryRange.ALL) {}
        } }
        compose.onNodeWithText("暂无其他棋手").assertIsDisplayed()
        compose.onNodeWithText("暂无棋手库对局").assertIsDisplayed()
    }
}
