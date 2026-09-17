package dev.goelo.android.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import dev.goelo.android.model.AppState
import dev.goelo.android.model.Match
import dev.goelo.android.model.MatchKind
import dev.goelo.android.model.Outcome
import dev.goelo.android.model.Profile
import dev.goelo.android.ui.settings.SettingsScreen
import dev.goelo.android.ui.theme.BlackGoldTheme
import org.junit.Rule
import org.junit.Test

class SettingsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun automaticTargetCanBeRestoredAndLowCustomTargetIsRejectedInUi() {
        val state = AppState(Profile(name = "棋手", initialElo = 2000.0), emptyList())
        compose.setContent { BlackGoldTheme {
            SettingsScreen(state, 1L, false, null, { _, _ -> }, { _, _ -> }, { _, _ -> })
        } }
        compose.onNodeWithText("恢复自动目标").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("设置目标").assertIsNotEnabled()
    }

    @Test fun achievedGoalAndLegacyInitialEloGuardAreVisible() {
        val legacy = Match("old", 1L, MatchKind.LEGACY, null, null, Outcome.WIN, null, null, 2000.0, 10.0, 2110.0, "legacy-fixed-v1")
        val profile = Profile(name = "棋手", initialElo = 2000.0, targetElo = 2100.0, targetStartElo = 2000.0)
        compose.setContent { BlackGoldTheme {
            SettingsScreen(AppState(profile, listOf(legacy)), 1L, false, null, { _, _ -> }, { _, _ -> }, { _, _ -> })
        } }
        compose.onNodeWithText("目标已达成").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("重新计算").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("旧历史的起点由原始分差和导入时的当前分推导，保持固定。").performScrollTo().assertIsDisplayed()
    }
}
