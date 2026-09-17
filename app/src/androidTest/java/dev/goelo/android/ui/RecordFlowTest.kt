package dev.goelo.android.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.goelo.android.data.MatchService
import dev.goelo.android.data.ProfileService
import dev.goelo.android.data.memoryStore
import dev.goelo.android.ui.theme.BlackGoldTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RecordFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun recordInputIsRequiredAndCanBeSaved() {
        val store = memoryStore()
        runBlocking { ProfileService(store).create("小林", 2200.0) }
        val model = AppViewModel(
            store, MatchService(store), ProfileService(store),
            Clock.fixed(Instant.ofEpochMilli(1000), ZoneOffset.UTC), { "match-1" },
        )
        compose.setContent { BlackGoldTheme { GoEloApp(model) } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("记一盘").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("记一盘").performClick()
        compose.onNodeWithTag("submit-win").assertIsNotEnabled()
        compose.onNodeWithTag("opponent-record").performTextInput("11-8")
        compose.onNodeWithTag("opponent-elo").assertTextEquals("2255.3")
        compose.onNodeWithTag("submit-win").assertIsEnabled().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("record-sheet").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("撤销").assertIsDisplayed()
    }
}
