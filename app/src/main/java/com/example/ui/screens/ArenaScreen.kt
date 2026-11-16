package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GameProfile
import com.example.data.GameType
import com.example.engine.AdaptiveDifficultyEngine
import com.example.engine.AiDecisionTelemetry
import com.example.engine.CellOwner
import com.example.engine.CyberSnakeState
import com.example.engine.GridPoint
import com.example.engine.Matrix2048State
import com.example.engine.TacticalGridState
import com.example.ui.AiTaskState
import com.example.ui.ApexMindViewModel
import com.example.ui.components.GameSelectorBar
import com.example.ui.components.GameStrategyDashboard
import com.example.ui.components.ResearchLabModal
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralMistake
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedCardSurface
import com.example.ui.theme.EmeraldTelemetry
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.PurpleNeural
import com.example.ui.theme.SlateSurface
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArenaScreen(
    viewModel: ApexMindViewModel,
    onNavigateToResearch: () -> Unit,
    onNavigateToMemory: () -> Unit
) {
    val selectedGame by viewModel.selectedGame.collectAsStateWithLifecycle()
    val profiles by viewModel.gameProfiles.collectAsStateWithLifecycle()
    val researchDocs by viewModel.researchDocs.collectAsStateWithLifecycle()
    val stateMemories by viewModel.stateMemories.collectAsStateWithLifecycle()
    val isAutoPlaying by viewModel.isAutoPlaying.collectAsStateWithLifecycle()
    val speedMultiplier by viewModel.playbackSpeedMultiplier.collectAsStateWithLifecycle()
    val decisionLog by viewModel.decisionLog.collectAsStateWithLifecycle()
    val liveAdvice by viewModel.liveAdvice.collectAsStateWithLifecycle()
    val aiTaskState by viewModel.aiTaskState.collectAsStateWithLifecycle()
    val researchModalState by viewModel.researchModalState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    var showResearchModal by rememberSaveable { mutableStateOf(false) }

    val tacticalState by viewModel.tacticalGridState.collectAsStateWithLifecycle()
    val matrix2048State by viewModel.matrix2048State.collectAsStateWithLifecycle()
    val cyberSnakeState by viewModel.cyberSnakeState.collectAsStateWithLifecycle()

    val activeProfile = profiles.firstOrNull { it.gameId == selectedGame.id } ?: GameProfile(
        gameId = selectedGame.id,
        difficultyLevel = 2L
    )
    val diffParams = AdaptiveDifficultyEngine.getParametersForLevel(activeProfile.difficultyLevel.toInt())
    val gameMemoriesCount = stateMemories.count { it.gameId == selectedGame.id }
    val gameResearchCount = researchDocs.count { it.gameId == selectedGame.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 680.dp)) {
            // Accessible Flow-Wrapped Game Switcher (all 3 games fully visible & reachable)
            GameSelectorBar(
                selectedGame = selectedGame,
                onSelectGame = { viewModel.selectGame(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Adaptive Difficulty & Agent Status Strip
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, BorderSubtle),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(tween(220, easing = FastOutSlowInEasing))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = diffParams.tierName.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                            Text(
                                text = selectedGame.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianBackground,
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text(
                                text = selectedGame.genreTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill(
                            label = "WIN RATE",
                            value = "${activeProfile.recentWinRatePercent}%",
                            accent = EmeraldTelemetry,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = "MEMORIES",
                            value = "$gameMemoriesCount",
                            accent = PurpleNeural,
                            onClick = onNavigateToMemory,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = "RESEARCH",
                            value = "$gameResearchCount",
                            accent = AmberWarning,
                            onClick = onNavigateToResearch,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = activeProfile.lastAdjustmentReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status / Post-Match Adaptation Banner
            AnimatedVisibility(
                visible = statusBanner != null,
                enter = fadeIn(tween(220)) + expandVertically(tween(240, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(160)) + shrinkVertically(tween(180, easing = FastOutSlowInEasing))
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = EmeraldTelemetry.copy(alpha = 0.13f)
                    ),
                    border = BorderStroke(1.dp, EmeraldTelemetry.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clickable { viewModel.dismissBanner() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Match Calibration",
                            tint = EmeraldTelemetry,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = statusBanner ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // AI Task Status Banner (Gemini Research / Consult / Autopsy)
            AnimatedContent(
                targetState = aiTaskState,
                transitionSpec = {
                    (fadeIn(tween(200)) + expandVertically(tween(220, easing = FastOutSlowInEasing))) togetherWith
                        (fadeOut(tween(150)) + shrinkVertically(tween(160, easing = FastOutSlowInEasing)))
                },
                label = "arena_ai_task_banner"
            ) { task ->
                when (task) {
                    is AiTaskState.Running -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = task.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = ElectricCyan,
                                    trackColor = ObsidianBackground
                                )
                            }
                        }
                    }
                    is AiTaskState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = CoralMistake.copy(alpha = 0.14f)
                            ),
                            border = BorderStroke(1.dp, CoralMistake.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clickable { viewModel.clearAiTaskState() }
                        ) {
                            Text(
                                text = task.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = CoralMistake,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    is AiTaskState.Success -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = ElectricCyan.copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clickable { viewModel.clearAiTaskState() }
                        ) {
                            Text(
                                text = task.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = ElectricCyan,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    AiTaskState.Idle -> Spacer(modifier = Modifier.height(0.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Game Strategy Dashboard (Current Target + Live Firestore Strategic Objectives)
            val dashboardVisibilityState = remember {
                MutableTransitionState(false).apply { targetState = true }
            }
            AnimatedVisibility(
                visibleState = dashboardVisibilityState,
                enter = fadeIn(animationSpec = tween(240)) +
                    expandVertically(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        expandFrom = Alignment.Top
                    ),
                exit = fadeOut(animationSpec = tween(180)) +
                    shrinkVertically(
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                        shrinkTowards = Alignment.Top
                    )
            ) {
                AnimatedContent(
                    targetState = selectedGame,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220)) +
                            expandVertically(
                                animationSpec = tween(240, easing = FastOutSlowInEasing),
                                expandFrom = Alignment.Top
                            )) togetherWith
                            (fadeOut(animationSpec = tween(180)) +
                                shrinkVertically(
                                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                                    shrinkTowards = Alignment.Top
                                ))
                    },
                    label = "arena_strategy_dashboard_game_transition"
                ) { targetGame ->
                    val targetProfile = profiles.firstOrNull { it.gameId == targetGame.id } ?: GameProfile(
                        gameId = targetGame.id,
                        difficultyLevel = 2L
                    )
                    GameStrategyDashboard(
                        selectedGame = targetGame,
                        profile = targetProfile,
                        researchDocs = researchDocs,
                        onToggleObjective = { objective ->
                            viewModel.toggleStrategicObjective(targetGame, objective)
                        },
                        onNavigateToResearch = {
                            viewModel.resetResearchModalState()
                            showResearchModal = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Game Board Viewport (smooth transition between all 3 playable games)
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(ElectricCyan.copy(alpha = 0.55f), PurpleNeural.copy(alpha = 0.38f))
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .animateContentSize(tween(240, easing = FastOutSlowInEasing))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = selectedGame,
                        transitionSpec = {
                            (fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                                scaleIn(
                                    initialScale = 0.98f,
                                    animationSpec = tween(240, easing = FastOutSlowInEasing)
                                )) togetherWith fadeOut(tween(150))
                        },
                        label = "game_board_switch_transition"
                    ) { game ->
                        when (game) {
                            GameType.TACTICAL_GRID -> TacticalGridBoardView(
                                state = tacticalState,
                                onSectorClicked = { r, c ->
                                    viewModel.stepSingleTurn(forcedAction = "SECTOR_${r}_${c}")
                                },
                                onResetMatch = { viewModel.resetCurrentMatch() }
                            )
                            GameType.NEURAL_2048 -> Matrix2048BoardView(
                                state = matrix2048State,
                                onDirectionClicked = { dir ->
                                    viewModel.stepSingleTurn(forcedAction = dir)
                                },
                                onResetMatch = { viewModel.resetCurrentMatch() }
                            )
                            GameType.CYBER_SNAKE -> CyberSnakeBoardView(
                                state = cyberSnakeState,
                                onDirectionClicked = { dir ->
                                    viewModel.stepSingleTurn(forcedAction = dir)
                                },
                                onResetMatch = { viewModel.resetCurrentMatch() }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Playback & AI Reasoning Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.toggleAutoPlay() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAutoPlaying) AmberWarning else ElectricCyan,
                        contentColor = Color(0xFF001F26)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1.35f)
                        .height(48.dp)
                        .testTag("toggle_autoplay_button")
                ) {
                    Icon(
                        imageVector = if (isAutoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isAutoPlaying) "Pause AI" else "Start AI Play",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAutoPlaying) "PAUSE AI" else "AUTO-PLAY AI",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = { viewModel.stepSingleTurn() },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("step_turn_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Step One Turn",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("STEP", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        val nextSpeed = when (speedMultiplier) {
                            1 -> 2
                            2 -> 4
                            else -> 1
                        }
                        viewModel.setPlaybackSpeed(nextSpeed)
                    },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("speed_toggle_button")
                ) {
                    Text("${speedMultiplier}x", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = { viewModel.resetCurrentMatch() },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("reset_match_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Match",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Deep Gemini Actions Row (Pre-Game Research Shortcut, Live Consult, Post-Match Mistake Autopsy)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToResearch,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("open_pregame_research_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Pre-Game Research",
                        tint = AmberWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Research First", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { viewModel.consultGeminiOnCurrentBoard(executeMoveImmediately = true) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("gemini_live_consult_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Ask Gemini Move",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gemini Move", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { viewModel.runDeepAiAutopsyOnLastMatch() },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("gemini_autopsy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Reflect & Learn",
                        tint = EmeraldTelemetry,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Learn From Turn", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }

            // Live Gemini Advice Card (smoothly revealed when requested)
            AnimatedVisibility(
                visible = liveAdvice != null,
                enter = fadeIn(tween(240)) + expandVertically(tween(260, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(180, easing = FastOutSlowInEasing))
            ) {
                liveAdvice?.let { advice ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "GEMINI CONSULT // ${advice.recommendedAction}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ElectricCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${advice.confidencePercent}% CONF",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldTelemetry
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = advice.reasoning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Risk Watch: ${advice.riskAssessment}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberWarning
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live AI State-Action-Outcome Telemetry & Memory Lookup Feed
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, BorderSubtle),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(tween(240, easing = FastOutSlowInEasing))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = "Live Telemetry",
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LIVE MEMORY TELEMETRY",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${decisionLog.size} turns",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    AnimatedContent(
                        targetState = decisionLog.isEmpty(),
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        label = "telemetry_feed_empty_toggle"
                    ) { isEmpty ->
                        if (isEmpty) {
                            Text(
                                text = "Tap directly on a board or press AUTO-PLAY AI / STEP to watch the AI encode board states, query its episodic memory bank, avoid past mistakes, and adapt difficulty.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                decisionLog.take(5).forEach { step ->
                                    DecisionTelemetryItem(step = step)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showResearchModal) {
        ResearchLabModal(
            selectedGame = selectedGame,
            modalState = researchModalState,
            onFetchStrategy = { gameTitle, externalSource, sourceType ->
                viewModel.fetchExternalStrategyViaGemini(
                    game = selectedGame,
                    gameTitle = gameTitle,
                    externalSource = externalSource,
                    sourceType = sourceType
                )
            },
            onOpenFullLab = {
                showResearchModal = false
                viewModel.resetResearchModalState()
                onNavigateToResearch()
            },
            onDismiss = {
                showResearchModal = false
                viewModel.resetResearchModalState()
            }
        )
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val interactiveModifier = if (onClick != null) {
        Modifier
            .clickable(onClick = onClick)
            .semantics {
                this.role = Role.Button
                this.contentDescription = "$label: $value"
            }
    } else {
        Modifier
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .then(interactiveModifier)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun DecisionTelemetryItem(step: AiDecisionTelemetry) {
    val borderAccent = when {
        step.avoidedMistakeMemory != null -> EmeraldTelemetry
        step.wasMistakeCommitted -> CoralMistake
        step.recalledMemory != null -> ElectricCyan
        else -> BorderSubtle
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianBackground)
            .border(1.dp, borderAccent, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "T${step.turnNumber} • ${step.chosenAction}",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricCyan
            )
            val deltaText = if (step.immediateScoreDelta >= 0) "+${step.immediateScoreDelta} pts" else "${step.immediateScoreDelta} pts"
            Text(
                text = deltaText,
                style = MaterialTheme.typography.labelMedium,
                color = if (step.immediateScoreDelta >= 0) EmeraldTelemetry else CoralMistake
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = step.reasoningHeadline,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "State Key: ${step.stateSignature}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BoardControlHint(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianBackground.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ElectricCyan,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MatchCompleteBar(
    isWin: Boolean,
    headline: String,
    onPlayAgain: () -> Unit
) {
    val accent = if (isWin) EmeraldTelemetry else AmberWarning
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = headline,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalButton(
            onClick = onPlayAgain,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = accent.copy(alpha = 0.25f),
                contentColor = accent
            ),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Play Again",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Play Again", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun DirectionDPad(
    tagPrefix: String,
    enabled: Boolean,
    onDirectionClicked: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 340.dp)
    ) {
        DirectionButton(
            label = "UP",
            icon = Icons.Default.ArrowUpward,
            enabled = enabled,
            onClick = { onDirectionClicked("UP") },
            modifier = Modifier
                .fillMaxWidth(0.36f)
                .testTag("${tagPrefix}_up")
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DirectionButton(
                label = "LEFT",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                enabled = enabled,
                onClick = { onDirectionClicked("LEFT") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("${tagPrefix}_left")
            )
            DirectionButton(
                label = "DOWN",
                icon = Icons.Default.ArrowDownward,
                enabled = enabled,
                onClick = { onDirectionClicked("DOWN") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("${tagPrefix}_down")
            )
            DirectionButton(
                label = "RIGHT",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                enabled = enabled,
                onClick = { onDirectionClicked("RIGHT") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("${tagPrefix}_right")
            )
        }
    }
}

@Composable
private fun DirectionButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (enabled) ElectricCyan.copy(alpha = 0.45f) else BorderSubtle),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = "Move $label" }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
private fun TacticalGridBoardView(
    state: TacticalGridState,
    onSectorClicked: (Int, Int) -> Unit,
    onResetMatch: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI: ${state.aiScore} PTS",
                style = MaterialTheme.typography.titleMedium,
                color = ElectricCyan
            )
            Text(
                text = "TURN ${state.turn}/${state.maxTurns}",
                style = MaterialTheme.typography.labelMedium,
                color = AmberWarning
            )
            Text(
                text = "BOT: ${state.botScore} PTS",
                style = MaterialTheme.typography.titleMedium,
                color = CoralMistake
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = state.lastBotAction,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        BoardControlHint(
            icon = Icons.Default.TouchApp,
            text = "Tap any open sector to claim it, or run AUTO-PLAY AI"
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
        ) {
            for (r in 0 until 5) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (c in 0 until 5) {
                        val cell = state.cellAt(r, c)
                        val targetBgColor = when (cell.owner) {
                            CellOwner.AI -> if (cell.isEmpTrap) CoralMistake.copy(alpha = 0.35f) else ElectricCyan.copy(alpha = 0.28f)
                            CellOwner.BOT -> PurpleNeural.copy(alpha = 0.28f)
                            CellOwner.NONE -> ObsidianBackground
                        }
                        val targetBorderColor = when (cell.owner) {
                            CellOwner.AI -> if (cell.isEmpTrap) CoralMistake else ElectricCyan
                            CellOwner.BOT -> PurpleNeural
                            CellOwner.NONE -> if (r == 2 && c == 2) AmberWarning else BorderSubtle
                        }
                        val animatedBg by animateColorAsState(
                            targetValue = targetBgColor,
                            animationSpec = tween(220),
                            label = "tg_bg_${r}_${c}"
                        )
                        val animatedBorder by animateColorAsState(
                            targetValue = targetBorderColor,
                            animationSpec = tween(220),
                            label = "tg_border_${r}_${c}"
                        )

                        val sectorDesc = when {
                            cell.isTrapRevealed && cell.isEmpTrap -> "Sector $r $c, EMP Trap triggered"
                            cell.owner == CellOwner.AI -> "Sector $r $c, captured by AI, +${cell.baseEnergy}"
                            cell.owner == CellOwner.BOT -> "Sector $r $c, captured by Bot, +${cell.baseEnergy}"
                            else -> "Open sector $r $c, +${cell.baseEnergy} energy"
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(animatedBg)
                                .border(1.5.dp, animatedBorder, RoundedCornerShape(10.dp))
                                .clickable(enabled = cell.owner == CellOwner.NONE && !state.isGameOver) {
                                    onSectorClicked(r, c)
                                }
                                .semantics { contentDescription = sectorDesc }
                                .testTag("tactical_cell_${r}_${c}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (cell.isTrapRevealed && cell.isEmpTrap) "EMP!" else "+${cell.baseEnergy}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = when {
                                        cell.isTrapRevealed && cell.isEmpTrap -> CoralMistake
                                        cell.owner == CellOwner.AI -> ElectricCyan
                                        cell.owner == CellOwner.BOT -> PurpleNeural
                                        else -> Color.White
                                    }
                                )
                                Text(
                                    text = when (cell.owner) {
                                        CellOwner.AI -> "AI"
                                        CellOwner.BOT -> "BOT"
                                        CellOwner.NONE -> if (r == 2 && c == 2) "CORE" else "($r,$c)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (cell.owner == CellOwner.NONE && r == 2 && c == 2) {
                                        AmberWarning
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.isGameOver,
            enter = fadeIn(tween(220)) + expandVertically(tween(220))
        ) {
            val won = state.aiScore >= state.botScore
            MatchCompleteBar(
                isWin = won,
                headline = if (won) {
                    "VICTORY • ${state.aiScore} vs ${state.botScore} PTS"
                } else {
                    "MATCH COMPLETE • ${state.aiScore} vs ${state.botScore} PTS"
                },
                onPlayAgain = onResetMatch
            )
        }
    }
}

@Composable
private fun Matrix2048BoardView(
    state: Matrix2048State,
    onDirectionClicked: (String) -> Unit,
    onResetMatch: () -> Unit
) {
    var dragDx by remember { mutableFloatStateOf(0f) }
    var dragDy by remember { mutableFloatStateOf(0f) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SCORE: ${state.score}",
                style = MaterialTheme.typography.titleMedium,
                color = ElectricCyan
            )
            Text(
                text = "MAX: ${state.maxTile} / ${state.targetTile}",
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldTelemetry
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        BoardControlHint(
            icon = Icons.Default.Swipe,
            text = "Swipe board or use arrows to slide tiles"
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianBackground)
                .pointerInput(state.turn, state.isGameOver) {
                    detectDragGestures(
                        onDragStart = {
                            dragDx = 0f
                            dragDy = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDx += dragAmount.x
                            dragDy += dragAmount.y
                        },
                        onDragEnd = {
                            if (!state.isGameOver && (abs(dragDx) > 36f || abs(dragDy) > 36f)) {
                                val dir = if (abs(dragDx) > abs(dragDy)) {
                                    if (dragDx > 0f) "RIGHT" else "LEFT"
                                } else {
                                    if (dragDy > 0f) "DOWN" else "UP"
                                }
                                onDirectionClicked(dir)
                            }
                            dragDx = 0f
                            dragDy = 0f
                        }
                    )
                }
                .semantics {
                    contentDescription = "2048 Matrix board, score ${state.score}, highest tile ${state.maxTile}"
                }
                .padding(10.dp)
        ) {
            for (r in 0 until 4) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (c in 0 until 4) {
                        val value = state.board[r * 4 + c]
                        val isCornerAnchor = r == 0 && c == 0 && value == state.maxTile && value > 0
                        val targetTileColor = when (value) {
                            0 -> SlateSurface
                            2, 4 -> Color(0xFF1E293B)
                            8, 16 -> Color(0xFF0E4F63)
                            32, 64 -> Color(0xFF006978)
                            128, 256 -> Color(0xFF00897B)
                            else -> Color(0xFF00BFA5)
                        }
                        val animatedTileColor by animateColorAsState(
                            targetValue = targetTileColor,
                            animationSpec = tween(180),
                            label = "m2048_bg_${r}_${c}"
                        )
                        val animatedBorderColor by animateColorAsState(
                            targetValue = if (isCornerAnchor) EmeraldTelemetry else BorderSubtle,
                            animationSpec = tween(180),
                            label = "m2048_border_${r}_${c}"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(animatedTileColor)
                                .border(
                                    width = if (isCornerAnchor) 2.dp else 1.dp,
                                    color = animatedBorderColor,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (value > 0) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$value",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isCornerAnchor) {
                                        Text(
                                            text = "ANCHOR",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldTelemetry
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        DirectionDPad(
            tagPrefix = "matrix2048_dir",
            enabled = !state.isGameOver,
            onDirectionClicked = onDirectionClicked
        )

        AnimatedVisibility(
            visible = state.isGameOver,
            enter = fadeIn(tween(220)) + expandVertically(tween(220))
        ) {
            MatchCompleteBar(
                isWin = state.isWin,
                headline = if (state.isWin) {
                    "TARGET ${state.targetTile} REACHED • ${state.score} PTS"
                } else {
                    "GRID LOCKED • MAX TILE ${state.maxTile}"
                },
                onPlayAgain = onResetMatch
            )
        }
    }
}

@Composable
private fun CyberSnakeBoardView(
    state: CyberSnakeState,
    onDirectionClicked: (String) -> Unit,
    onResetMatch: () -> Unit
) {
    var dragDx by remember { mutableFloatStateOf(0f) }
    var dragDy by remember { mutableFloatStateOf(0f) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SCORE: ${state.score}",
                style = MaterialTheme.typography.titleMedium,
                color = ElectricCyan
            )
            Text(
                text = "CORES: ${state.coresCollected}/${state.targetCores}",
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldTelemetry
            )
            Text(
                text = "HAZARDS: ${state.firewalls.size}",
                style = MaterialTheme.typography.labelMedium,
                color = CoralMistake
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        BoardControlHint(
            icon = Icons.Default.Swipe,
            text = "Swipe grid or tap arrows to steer around firewalls"
        )
        Spacer(modifier = Modifier.height(10.dp))

        val head = state.snakeBody.firstOrNull()
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 350.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianBackground)
                .pointerInput(state.turn, state.isGameOver) {
                    detectDragGestures(
                        onDragStart = {
                            dragDx = 0f
                            dragDy = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDx += dragAmount.x
                            dragDy += dragAmount.y
                        },
                        onDragEnd = {
                            if (!state.isGameOver && (abs(dragDx) > 36f || abs(dragDy) > 36f)) {
                                val dir = if (abs(dragDx) > abs(dragDy)) {
                                    if (dragDx > 0f) "RIGHT" else "LEFT"
                                } else {
                                    if (dragDy > 0f) "DOWN" else "UP"
                                }
                                onDirectionClicked(dir)
                            }
                            dragDx = 0f
                            dragDy = 0f
                        }
                    )
                }
                .semantics {
                    contentDescription = "Cyber Snake 8 by 8 arena, ${state.coresCollected} of ${state.targetCores} cores collected"
                }
                .padding(8.dp)
        ) {
            for (r in 0 until state.gridSize) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (c in 0 until state.gridSize) {
                        val pt = GridPoint(r, c)
                        val isHead = pt == head
                        val isBody = !isHead && pt in state.snakeBody
                        val isCore = pt == state.dataCore
                        val isFirewall = pt in state.firewalls

                        val targetCellColor = when {
                            isHead -> ElectricCyan
                            isBody -> ElectricCyan.copy(alpha = 0.45f)
                            isCore -> EmeraldTelemetry
                            isFirewall -> CoralMistake.copy(alpha = 0.85f)
                            else -> SlateSurface
                        }
                        val animatedCellColor by animateColorAsState(
                            targetValue = targetCellColor,
                            animationSpec = tween(150),
                            label = "snake_cell_${r}_${c}"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(animatedCellColor),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isHead -> Text(
                                    text = "AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianBackground
                                )
                                isCore -> Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Data Core",
                                    tint = ObsidianBackground,
                                    modifier = Modifier.size(14.dp)
                                )
                                isFirewall -> Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Firewall Hazard",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        DirectionDPad(
            tagPrefix = "snake_dir",
            enabled = !state.isGameOver,
            onDirectionClicked = onDirectionClicked
        )

        AnimatedVisibility(
            visible = state.isGameOver,
            enter = fadeIn(tween(220)) + expandVertically(tween(220))
        ) {
            MatchCompleteBar(
                isWin = state.isWin,
                headline = if (state.isWin) {
                    "ALL ${state.targetCores} CORES HARVESTED • ${state.score} PTS"
                } else {
                    "${state.statusNote.uppercase()} • ${state.coresCollected}/${state.targetCores} CORES"
                },
                onPlayAgain = onResetMatch
            )
        }
    }
}
