package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.ResearchModalState
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 680.dp)) {
            // Game Switcher Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameType.entries.forEach { game ->
                    val selected = game == selectedGame
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectGame(game) },
                        label = {
                            Text(
                                text = game.title,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                            selectedLabelColor = ElectricCyan
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_game_${game.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Adaptive Difficulty & Agent Status Strip
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricPill(
                                label = "WIN RATE",
                                value = "${activeProfile.recentWinRatePercent}%",
                                accent = EmeraldTelemetry
                            )
                            MetricPill(
                                label = "MEMORIES",
                                value = "$gameMemoriesCount",
                                accent = PurpleNeural,
                                onClick = onNavigateToMemory
                            )
                            MetricPill(
                                label = "RESEARCH",
                                value = "$gameResearchCount",
                                accent = AmberWarning,
                                onClick = onNavigateToResearch
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = activeProfile.lastAdjustmentReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status / Post-Match Adaptation Banner
            AnimatedVisibility(visible = statusBanner != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = EmeraldTelemetry.copy(alpha = 0.14f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .border(1.dp, EmeraldTelemetry.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
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
            when (val task = aiTaskState) {
                is AiTaskState.Running -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface),
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
                                modifier = Modifier.fillMaxWidth(),
                                color = ElectricCyan
                            )
                        }
                    }
                }
                is AiTaskState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CoralMistake.copy(alpha = 0.15f)
                        ),
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
                            containerColor = ElectricCyan.copy(alpha = 0.14f)
                        ),
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
                AiTaskState.Idle -> Unit
            }

            Spacer(modifier = Modifier.height(10.dp))

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

            // Interactive Game Board Viewport
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(ElectricCyan.copy(alpha = 0.6f), PurpleNeural.copy(alpha = 0.4f))
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (selectedGame) {
                        GameType.TACTICAL_GRID -> TacticalGridBoardView(
                            state = tacticalState,
                            onSectorClicked = { r, c ->
                                viewModel.stepSingleTurn(forcedAction = "SECTOR_${r}_${c}")
                            }
                        )
                        GameType.NEURAL_2048 -> Matrix2048BoardView(
                            state = matrix2048State,
                            onDirectionClicked = { dir ->
                                viewModel.stepSingleTurn(forcedAction = dir)
                            }
                        )
                        GameType.CYBER_SNAKE -> CyberSnakeBoardView(
                            state = cyberSnakeState,
                            onDirectionClicked = { dir ->
                                viewModel.stepSingleTurn(forcedAction = dir)
                            }
                        )
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
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("toggle_autoplay_button")
                ) {
                    Icon(
                        imageVector = if (isAutoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isAutoPlaying) "Pause AI" else "Start AI Play"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAutoPlaying) "PAUSE AI" else "AUTO-PLAY AI",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                FilledTonalButton(
                    onClick = { viewModel.stepSingleTurn() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("step_turn_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Step One Turn"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("STEP", style = MaterialTheme.typography.labelLarge)
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
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("speed_toggle_button")
                ) {
                    Text("${speedMultiplier}x", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = { viewModel.resetCurrentMatch() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("reset_match_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Match"
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
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("open_pregame_research_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Pre-Game Research",
                        tint = AmberWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Research First", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = { viewModel.consultGeminiOnCurrentBoard(executeMoveImmediately = true) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("gemini_live_consult_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Ask Gemini Move",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gemini Move", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = { viewModel.runDeepAiAutopsyOnLastMatch() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("gemini_autopsy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Reflect & Learn",
                        tint = EmeraldTelemetry,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Learn From Turn", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Live Gemini Advice Card (if requested)
            if (liveAdvice != null) {
                val advice = liveAdvice!!
                Card(
                    colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "GEMINI TACTICAL CONSULT // ${advice.recommendedAction}",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                            Text(
                                text = "${advice.confidencePercent}% CONFIDENCE",
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

            Spacer(modifier = Modifier.height(14.dp))

            // Live AI State-Action-Outcome Telemetry & Memory Lookup Feed
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = "Live Telemetry",
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LIVE STATE-ACTION MEMORY TELEMETRY",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${decisionLog.size} turns logged",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (decisionLog.isEmpty()) {
                        Text(
                            text = "Press AUTO-PLAY AI or STEP to watch the AI encode board states, query its episodic memory bank, avoid past mistakes, and adapt difficulty.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        decisionLog.take(5).forEach { step ->
                            DecisionTelemetryItem(step = step)
                            Spacer(modifier = Modifier.height(8.dp))
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
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.14f))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
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
private fun TacticalGridBoardView(
    state: TacticalGridState,
    onSectorClicked: (Int, Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "APEXMIND AI: ${state.aiScore} PTS",
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
                        val bgColor = when (cell.owner) {
                            CellOwner.AI -> if (cell.isEmpTrap) CoralMistake.copy(alpha = 0.35f) else ElectricCyan.copy(alpha = 0.28f)
                            CellOwner.BOT -> PurpleNeural.copy(alpha = 0.28f)
                            CellOwner.NONE -> ObsidianBackground
                        }
                        val borderColor = when (cell.owner) {
                            CellOwner.AI -> if (cell.isEmpTrap) CoralMistake else ElectricCyan
                            CellOwner.BOT -> PurpleNeural
                            CellOwner.NONE -> if (r == 2 && c == 2) AmberWarning else BorderSubtle
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bgColor)
                                .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
                                .clickable(enabled = cell.owner == CellOwner.NONE && !state.isGameOver) {
                                    onSectorClicked(r, c)
                                },
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
                                        CellOwner.NONE -> "(${r},${c})"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Matrix2048BoardView(
    state: Matrix2048State,
    onDirectionClicked: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SCORE: ${state.score}",
                style = MaterialTheme.typography.titleMedium,
                color = ElectricCyan
            )
            Text(
                text = "MAX: ${state.maxTile} / TARGET: ${state.targetTile}",
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldTelemetry
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianBackground)
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
                        val tileColor = when (value) {
                            0 -> SlateSurface
                            2, 4 -> Color(0xFF1E293B)
                            8, 16 -> Color(0xFF0E4F63)
                            32, 64 -> Color(0xFF006978)
                            128, 256 -> Color(0xFF00897B)
                            else -> Color(0xFF00BFA5)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(tileColor)
                                .border(
                                    width = if (isCornerAnchor) 2.dp else 1.dp,
                                    color = if (isCornerAnchor) EmeraldTelemetry else BorderSubtle,
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

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("UP", "LEFT", "RIGHT", "DOWN").forEach { dir ->
                OutlinedButton(
                    onClick = { onDirectionClicked(dir) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(dir, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun CyberSnakeBoardView(
    state: CyberSnakeState,
    onDirectionClicked: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
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
                text = "FIREWALLS: ${state.firewalls.size}",
                style = MaterialTheme.typography.labelMedium,
                color = CoralMistake
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val head = state.snakeBody.firstOrNull()
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 350.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianBackground)
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

                        val cellColor = when {
                            isHead -> ElectricCyan
                            isBody -> ElectricCyan.copy(alpha = 0.45f)
                            isCore -> EmeraldTelemetry
                            isFirewall -> CoralMistake.copy(alpha = 0.8f)
                            else -> SlateSurface
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cellColor),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isHead -> Text("AI", style = MaterialTheme.typography.labelSmall, color = ObsidianBackground)
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

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("UP", "LEFT", "RIGHT", "DOWN").forEach { dir ->
                OutlinedButton(
                    onClick = { onDirectionClicked(dir) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(dir, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
