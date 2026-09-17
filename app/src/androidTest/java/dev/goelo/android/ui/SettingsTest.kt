package dev.goelo.android.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.goelo.android.model.AppState
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
        compose.onNodeWithText("恢复自动目标").assertIsDisplayed()
        compose.onNodeWithText("设置目标").assertIsNotEnabled()
    }
}
