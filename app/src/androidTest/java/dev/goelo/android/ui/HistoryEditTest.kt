package dev.goelo.android.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.goelo.android.data.MatchService
import dev.goelo.android.data.ProfileService
import dev.goelo.android.data.memoryStore
import dev.goelo.android.model.Outcome
import dev.goelo.android.model.RecordInput
import dev.goelo.android.model.Match
import dev.goelo.android.model.MatchKind
import dev.goelo.android.rating.scoreChange
import dev.goelo.android.ui.history.HistoryScreen
import dev.goelo.android.ui.theme.BlackGoldTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryEditTest {
    @get:org.junit.Rule val compose = createComposeRule()
    @Test fun correctingFirstGameChangesLaterScore() = runTest {
        val store = memoryStore()
        try {
            ProfileService(store).create("棋手", 2000.0)
            val service = MatchService(store)
            service.record("a", RecordInput(6, 0, 0), Outcome.WIN, 1000L, "UTC")
            service.record("b", RecordInput(6, 0, 0), Outcome.WIN, 2000L, "UTC")
            service.edit("a", RecordInput(6, 0, 0), Outcome.LOSS, store.read().revision)
            val rows = store.read().state.matches.sortedBy { it.order }
            assertEquals(1990.0, rows[0].eloAfter, 0.0)
            assertEquals(1990.0, rows[1].eloBefore, 0.0)
            assertEquals(scoreChange(1990.0, 2000.0, Outcome.WIN).after, rows[1].eloAfter, 1e-7)
        } finally { store.close() }
    }

    @Test fun emptyHistoryAndLegacyReadOnlyExplanationAreVisible() {
        compose.setContent { BlackGoldTheme {
            HistoryScreen(emptyList(), 1L, false, null, { _, _, _, _ -> }, { _, _ -> })
        } }
        compose.onNodeWithText("没有符合条件的对局").assertIsDisplayed()
        compose.onNodeWithText("1 段").assertExists()
        compose.onNodeWithText("9 段").assertExists()

        val legacy = Match("old", 1L, MatchKind.LEGACY, null, null, Outcome.LOSS, null, null, 2000.0, -10.0, 1990.0, "legacy-fixed-v1")
        compose.setContent { BlackGoldTheme {
            HistoryScreen(listOf(legacy), 1L, false, null, { _, _, _, _ -> }, { _, _ -> })
        } }
        compose.onNodeWithText("只读").performClick()
        compose.onNodeWithText("旧版导入记录按原始变动保存，不能编辑或删除。如需调整，请在“我的”页面重新导入旧文件。").assertIsDisplayed()
    }

    @Test fun deleteConfirmationIdentifiesNativeMatchAndAffectedCount() {
        val native = Match("one", 1L, MatchKind.NATIVE, 0L, "UTC", Outcome.WIN, RecordInput(7, 11, 8), 2200.0, 2000.0, 10.0, 2010.0, "elo-v1")
        val later = Match("two", 2L, MatchKind.LEGACY, null, null, Outcome.LOSS, null, null, 2010.0, -10.0, 2000.0, "legacy-fixed-v1")
        compose.setContent { BlackGoldTheme {
            HistoryScreen(listOf(native, later), 5L, false, null, { _, _, _, _ -> }, { _, _ -> })
        } }
        compose.onNodeWithText("更正").performClick()
        compose.onNodeWithText("删除").performClick()
        compose.onNodeWithText("胜 · 7 段 · 战绩 11-8 · 1970-01-01 · +10.0", substring = true).assertIsDisplayed()
        compose.onNodeWithText("并重算后续 1 盘。", substring = true).assertIsDisplayed()
    }
}
