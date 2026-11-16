package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.GameProfile
import com.example.data.GameType
import com.example.engine.Matrix2048State
import com.example.engine.Neural2048Engine
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GameSelectorAndGamesAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun gameSelectorBar_displaysAllGames_andSwitchesSelection() {
        var selected by mutableStateOf(GameType.TACTICAL_GRID)

        composeTestRule.setContent {
            MyApplicationTheme {
                GameSelectorBar(
                    selectedGame = selected,
                    onSelectGame = { selected = it }
                )
            }
        }

        composeTestRule.waitForIdle()
        GameType.entries.forEach { game ->
            composeTestRule.onNodeWithTag("select_game_${game.id}").assertIsDisplayed()
            composeTestRule.onNodeWithText(game.title).assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag("select_game_${GameType.NEURAL_2048.id}").performClick()
        composeTestRule.waitForIdle()
        assertEquals(GameType.NEURAL_2048, selected)

        composeTestRule.onNodeWithTag("select_game_${GameType.CYBER_SNAKE.id}").performClick()
        composeTestRule.waitForIdle()
        assertEquals(GameType.CYBER_SNAKE, selected)
    }

    @Test
    fun neural2048Engine_blockedForcedDirectionDoesNotMoveOtherWay() {
        // Top row already packed left, sliding UP or LEFT produces no tile movement
        val board = listOf(
            2, 4, 8, 16,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0
        )
        val state = Matrix2048State(board = board, score = 0, maxTile = 16, targetTile = 128, turn = 1)
        val profile = GameProfile(gameId = GameType.NEURAL_2048.id, difficultyLevel = 2L)

        val (nextState, telemetry) = Neural2048Engine.stepAiTurn(
            state = state,
            profile = profile,
            memories = emptyList(),
            researchDocs = emptyList(),
            forcedAction = "UP"
        )

        assertEquals("NONE", telemetry.chosenAction)
        assertEquals(board, nextState.board)
    }
}
