package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.GameProfile
import com.example.data.GameType
import com.example.data.ResearchDoc
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GameStrategyDashboardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysTargetAndFirestoreObjectives_andTogglesObjective() {
        var toggledObjective: String? = null
        val profile = GameProfile(
            gameId = GameType.TACTICAL_GRID.id,
            difficultyLevel = 4L,
            targetWinRate = 70L,
            completedObjectives = listOf("Control center sector")
        )
        val docs = listOf(
            ResearchDoc(
                id = "doc_1",
                gameId = GameType.TACTICAL_GRID.id,
                sourceTitle = "Grid Wiki",
                keyObjectives = listOf("Control center sector", "Avoid red EMP traps")
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                GameStrategyDashboard(
                    selectedGame = GameType.TACTICAL_GRID,
                    profile = profile,
                    researchDocs = docs,
                    onToggleObjective = { toggledObjective = it },
                    onNavigateToResearch = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("game_strategy_dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("game_strategy_target_text")
            .assertTextContains("Outscore Tactical Bot in 14 turns", substring = true)
        composeTestRule.onNodeWithTag("strategy_objectives_counter")
            .assertTextContains("1 / 2 COMPLETED", substring = true)

        composeTestRule.onNodeWithTag("strategy_objective_row_1").performClick()
        composeTestRule.waitForIdle()
        assertEquals("Avoid red EMP traps", toggledObjective)
    }

    @Test
    fun emptyResearchDocs_showsTargetAndOpenResearchLabButton() {
        var navigatedToResearch = false
        val profile = GameProfile(
            gameId = GameType.NEURAL_2048.id,
            difficultyLevel = 2L,
            targetWinRate = 65L
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                GameStrategyDashboard(
                    selectedGame = GameType.NEURAL_2048,
                    profile = profile,
                    researchDocs = emptyList(),
                    onToggleObjective = {},
                    onNavigateToResearch = { navigatedToResearch = true }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("game_strategy_target_text")
            .assertTextContains("128 milestone tile", substring = true)
        composeTestRule.onNodeWithTag("dashboard_open_research_button")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertTrue(navigatedToResearch)
    }

    @Test
    fun transitionsFromEmptyToLoadedObjectives_andExpandsMoreRows() {
        var docs by mutableStateOf<List<ResearchDoc>>(emptyList())
        val profile = GameProfile(
            gameId = GameType.CYBER_SNAKE.id,
            difficultyLevel = 3L,
            targetWinRate = 68L,
            completedObjectives = listOf("Sweep perimeter lanes")
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                GameStrategyDashboard(
                    selectedGame = GameType.CYBER_SNAKE,
                    profile = profile,
                    researchDocs = docs,
                    onToggleObjective = {},
                    onNavigateToResearch = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dashboard_open_research_button").assertIsDisplayed()

        docs = listOf(
            ResearchDoc(
                id = "snake_doc",
                gameId = GameType.CYBER_SNAKE.id,
                sourceTitle = "Snake Routing Guide",
                keyObjectives = listOf(
                    "Sweep perimeter lanes",
                    "Reserve dual exit corridors",
                    "Track firewall spawn cycles",
                    "Spiral inward for final cores"
                )
            )
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dashboard_open_research_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("strategy_objectives_counter")
            .assertTextContains("1 / 4 COMPLETED", substring = true)
        composeTestRule.onNodeWithTag("strategy_objective_row_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("strategy_objective_row_3").assertDoesNotExist()

        composeTestRule.onNodeWithTag("strategy_dashboard_toggle_expand")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("strategy_objective_row_3").assertIsDisplayed()
    }
}
