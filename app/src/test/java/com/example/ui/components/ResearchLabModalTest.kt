package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.ai.HttpExternalStrategyFetcher
import com.example.data.GameProfile
import com.example.data.GameType
import com.example.ui.ResearchModalState
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
class ResearchLabModalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardEmptyState_opensResearchLabModal_andFetchesObjectivesForCustomGameTitle() {
        var modalVisible by mutableStateOf(false)
        var modalState by mutableStateOf<ResearchModalState>(ResearchModalState.Idle)
        var capturedTitle = ""
        var capturedSource = ""
        var capturedType = ""

        val profile = GameProfile(
            gameId = GameType.NEURAL_2048.id,
            difficultyLevel = 3L,
            targetWinRate = 65L
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                GameStrategyDashboard(
                    selectedGame = GameType.NEURAL_2048,
                    profile = profile,
                    researchDocs = emptyList(),
                    onToggleObjective = {},
                    onNavigateToResearch = {
                        modalState = ResearchModalState.Idle
                        modalVisible = true
                    }
                )
                if (modalVisible) {
                    ResearchLabModal(
                        selectedGame = GameType.NEURAL_2048,
                        modalState = modalState,
                        onFetchStrategy = { title, source, type ->
                            capturedTitle = title
                            capturedSource = source
                            capturedType = type
                            modalState = ResearchModalState.Success(
                                sourceTitle = "Wikipedia: $title",
                                summary = "Keep the max tile anchored in a corner and maintain monotonic chains.",
                                extractedObjectives = listOf(
                                    "Lock highest tile in top-left corner",
                                    "Maintain monotonic descending top row"
                                ),
                                fetchedFromWeb = true
                            )
                        },
                        onOpenFullLab = {},
                        onDismiss = { modalVisible = false }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dashboard_open_research_button")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("research_lab_modal").assertIsDisplayed()

        composeTestRule.onNodeWithTag("research_modal_game_title_input").performTextClearance()
        composeTestRule.onNodeWithTag("research_modal_game_title_input").performTextInput("2048 Strategy")
        composeTestRule.onNodeWithTag("research_modal_type_strategy_guide")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("research_modal_source_input")
            .performScrollTo()
            .performTextInput("https://en.wikipedia.org/wiki/2048_(video_game)")

        composeTestRule.onNodeWithTag("research_modal_fetch_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("2048 Strategy", capturedTitle)
        assertEquals("https://en.wikipedia.org/wiki/2048_(video_game)", capturedSource)
        assertEquals("strategy_guide", capturedType)

        composeTestRule.onNodeWithTag("research_modal_objectives_preview")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("EXTRACTED OBJECTIVES (2)", substring = true)
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("research_modal_done_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("research_lab_modal").assertDoesNotExist()
    }

    @Test
    fun modalErrorState_displaysErrorBanner_andSupportsOpenFullLab() {
        var openedFullLab = false
        var dismissed = false

        composeTestRule.setContent {
            MyApplicationTheme {
                ResearchLabModal(
                    selectedGame = GameType.TACTICAL_GRID,
                    modalState = ResearchModalState.Error("External URL returned HTTP 404"),
                    onFetchStrategy = { _, _, _ -> },
                    onOpenFullLab = { openedFullLab = true },
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("research_modal_error_banner")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("HTTP 404", substring = true)
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("research_modal_full_lab_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertTrue(dismissed)
        assertTrue(openedFullLab)
    }

    @Test
    fun externalFetcher_stripsHtmlAndExtractsTitle() {
        val fetcher = HttpExternalStrategyFetcher()
        val sampleHtml = """
            <html>
              <head>
                <title>2048 (video game) - Wikipedia</title>
                <script>var ignore = true;</script>
                <style>.hide { display: none; }</style>
              </head>
              <body>
                <h1>Gameplay &amp; Strategy</h1>
                <p>Keep the highest tile in a corner and build monotonic rows.</p>
              </body>
            </html>
        """.trimIndent()

        assertEquals("2048 (video game) - Wikipedia", fetcher.extractHtmlTitle(sampleHtml))
        val cleaned = fetcher.stripHtmlToText(sampleHtml)
        assertTrue(cleaned.contains("Gameplay & Strategy"))
        assertTrue(cleaned.contains("Keep the highest tile in a corner"))
        assertTrue(!cleaned.contains("var ignore"))
    }
}
