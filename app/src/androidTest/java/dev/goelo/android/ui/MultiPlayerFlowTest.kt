package dev.goelo.android.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.goelo.android.data.*
import dev.goelo.android.model.*
import dev.goelo.android.ui.theme.BlackGoldTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.util.UUID

class MultiPlayerFlowTest {
    @get:Rule val compose = createComposeRule()
    @Test fun addSelectOpponentRecordAndSwitchToMirroredHistory() {
        val store=memoryStore()
        runBlocking { ProfileService(store).create("甲",2000.0) }
        val model=AppViewModel(store,MatchService(store),ProfileService(store),Clock.systemUTC(),{ UUID.randomUUID().toString() })
        compose.setContent { BlackGoldTheme { GoEloApp(model) } }
        compose.waitUntil(5000) { model.ui.value.snapshot?.state?.profile != null }
        compose.onNodeWithTag("players-entry").performClick()
        compose.onNodeWithText("添加棋手").performClick()
        compose.onNodeWithTag("add-player-name").performScrollTo().performTextInput("乙")
        compose.onNodeWithTag("add-player-save").performScrollTo().performClick()
        compose.waitUntil(5000) { model.ui.value.snapshot?.state?.allProfiles?.size == 2 }
        compose.onNodeWithText("完成").performClick()
        compose.onNodeWithText("记一盘").performClick()
        compose.onNodeWithText("已有棋手").performClick()
        val other=model.ui.value.snapshot!!.state.allProfiles.single { it.name=="乙" }.id
        compose.onNodeWithTag("opponent-$other").performScrollTo().performClick()
        compose.onNodeWithTag("submit-win").performScrollTo().performClick()
        compose.waitUntil(5000) { !model.ui.value.recordOpen && !model.ui.value.busy }
        assertEquals(2010.0,model.ui.value.snapshot!!.state.ratingOf("local"),1e-8)
        compose.onNodeWithTag("players-entry").performClick()
        compose.onNodeWithTag("players-list").performScrollToNode(hasTestTag("select-player-$other"))
        compose.onNodeWithTag("select-player-$other").performClick()
        compose.waitUntil(5000) { model.ui.value.snapshot?.state?.profile?.id == other }
        compose.onNodeWithText("历史").performClick()
        compose.onNodeWithText("甲",substring=false).assertIsDisplayed()
        compose.onNodeWithText("-10.0").assertIsDisplayed()
        assertEquals(1990.0,model.ui.value.snapshot!!.state.ratingOf(other),1e-8)
        store.close()
    }
}
