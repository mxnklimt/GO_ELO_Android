package dev.goelo.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import dev.goelo.android.ui.onboarding.OnboardingScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OnboardingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `trimmed name and selected initial score create a profile`() {
        var received: Pair<String, Double>? = null
        compose.setContent { MaterialTheme { OnboardingScreen { name, elo -> received = name to elo } } }
        compose.onNodeWithTag("profile-name").performTextInput("  小林  ")
        compose.onNodeWithText("7段").performClick()
        compose.onNodeWithText("开始记录").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("小林" to 2300.0, received) }
    }
}
