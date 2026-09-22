package dev.goelo.android.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.goelo.android.ui.doglist.DogListScreen
import dev.goelo.android.ui.theme.BlackGoldTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class DogListScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun searchShowsMatchingWildFoxIds() {
        compose.setContent {
            BlackGoldTheme {
                DogListScreen(
                    ids = listOf("AlphaFox", "Beta", "fox-master"),
                    revision = 1L,
                    busy = false,
                    error = null,
                    onAdd = { _, _ -> },
                    onRemove = { _, _ -> },
                )
            }
        }
        compose.onNodeWithTag("dog-search").performTextInput("FOX")
        compose.onNodeWithText("AlphaFox").assertIsDisplayed()
        compose.onNodeWithText("fox-master").assertIsDisplayed()
        compose.onAllNodesWithText("Beta").assertCountEquals(0)
    }

    @Test fun addButtonSendsTrimmedIdAndDeleteButtonIsVisible() {
        var added: Pair<String, Long>? = null
        var removed: Pair<String, Long>? = null
        compose.setContent {
            BlackGoldTheme {
                DogListScreen(
                    ids = listOf("AlphaFox"),
                    revision = 7L,
                    busy = false,
                    error = null,
                    onAdd = { id, revision -> added = id to revision },
                    onRemove = { id, revision -> removed = id to revision },
                )
            }
        }
        compose.onNodeWithTag("dog-input").performTextInput("  NewFox  ")
        compose.onNodeWithTag("dog-add").performClick()
        assertEquals("NewFox" to 7L, added)
        compose.onNodeWithTag("dog-remove-AlphaFox").assertIsDisplayed()
        compose.onNodeWithTag("dog-remove-AlphaFox").performClick()
        assertEquals("AlphaFox" to 7L, removed)
    }
}
