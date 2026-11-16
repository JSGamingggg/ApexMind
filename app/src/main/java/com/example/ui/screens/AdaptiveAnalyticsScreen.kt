package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GameProfile
import com.example.data.GameType
import com.example.data.MatchEpisode
import com.example.engine.AdaptiveDifficultyEngine
import com.example.ui.ApexMindViewModel
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
fun AdaptiveAnalyticsScreen(
    viewModel: ApexMindViewModel
) {
    val selectedGame by viewModel.selectedGame.collectAsStateWithLifecycle()
    val profiles by viewModel.gameProfiles.collectAsStateWithLifecycle()
    val episodes by viewModel.matchEpisodes.collectAsStateWithLifecycle()

    val profile = profiles.firstOrNull { it.gameId == selectedGame.id } ?: GameProfile(
        gameId = selectedGame.id,
        difficultyLevel = 2L
    )
    val diffParams = AdaptiveDifficultyEngine.getParametersForLevel(profile.difficultyLevel.toInt())
    val gameEpisodes = episodes.filter { it.gameId == selectedGame.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 680.dp)) {
            Text(
                text = "ADAPTIVE DIFFICULTY & PERFORMANCE ENGINE",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Monitors the AI's rolling win/loss ratio and mistake rate to dynamically adjust game difficulty, maintaining an optimal learning challenge.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Game Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameType.entries.forEach { game ->
                    FilterChip(
                        selected = game == selectedGame,
                        onClick = { viewModel.selectGame(game) },
                        label = { Text(game.title, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                            selectedLabelColor = ElectricCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Adaptive Difficulty Calibration Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = diffParams.tierName.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricCyan
                            )
                            Text(
                                text = "Rolling Win Rate: ${profile.recentWinRatePercent}% • Lifetime: ${profile.totalWins}W / ${profile.totalLosses}L • Best: ${profile.bestScore} pts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AUTO",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (profile.autoAdjustDifficulty) EmeraldTelemetry else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = profile.autoAdjustDifficulty,
                                onCheckedChange = { enabled ->
                                    viewModel.toggleAutoAdjustDifficulty(selectedGame, enabled)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ObsidianBackground,
                                    checkedTrackColor = EmeraldTelemetry
                                ),
                                modifier = Modifier.testTag("auto_difficulty_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Rolling 10-Match Outcome Window Visualizer
                    Text(
                        text = "RECENT 10-MATCH OUTCOME WINDOW",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (profile.recentOutcomes.isEmpty()) {
                            Text(
                                text = "No recent matches recorded yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            profile.recentOutcomes.forEach { outcome ->
                                val isWin = outcome == "win"
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isWin) EmeraldTelemetry.copy(alpha = 0.22f)
                                            else CoralMistake.copy(alpha = 0.22f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isWin) EmeraldTelemetry else CoralMistake,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isWin) "W" else "L",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isWin) EmeraldTelemetry else CoralMistake
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Manual / Override Tier Selector (1..10)
                    Text(
                        text = "CALIBRATE CHALLENGE TIER (1–10)",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        (1..10).forEach { lvl ->
                            FilterChip(
                                selected = profile.difficultyLevel.toInt() == lvl,
                                onClick = { viewModel.setManualDifficultyLevel(selectedGame, lvl) },
                                label = { Text("T$lvl", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("difficulty_tier_$lvl")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Target Win Rate Slider (Zone of Proximal Development)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "OPTIMAL LEARNING TARGET WIN RATE",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldTelemetry
                        )
                        Text(
                            text = "${profile.targetWinRate}% (Band: ${profile.targetWinRate - 15}%–${profile.targetWinRate + 10}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                    Slider(
                        value = profile.targetWinRate.toFloat(),
                        onValueChange = { newVal ->
                            viewModel.updateTargetWinRate(selectedGame, newVal.toInt())
                        },
                        valueRange = 40f..85f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldTelemetry,
                            activeTrackColor = EmeraldTelemetry
                        ),
                        modifier = Modifier.testTag("target_winrate_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Active Engine Parameters at Current Tier
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianBackground)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        when (selectedGame) {
                            GameType.TACTICAL_GRID -> {
                                Text("EMP Traps: ${diffParams.tacticalTrapCount}", style = MaterialTheme.typography.labelMedium, color = CoralMistake)
                                Text("Bot Depth: ${diffParams.tacticalBotLookahead} ply", style = MaterialTheme.typography.labelMedium, color = PurpleNeural)
                                Text("Target Lead: +${diffParams.tacticalTargetLead}", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                            }
                            GameType.NEURAL_2048 -> {
                                Text("Target Tile: ${diffParams.targetTile2048}", style = MaterialTheme.typography.labelMedium, color = EmeraldTelemetry)
                                Text("4-Tile Spawn: ${diffParams.fourTileSpawnProbPercent}%", style = MaterialTheme.typography.labelMedium, color = AmberWarning)
                            }
                            GameType.CYBER_SNAKE -> {
                                Text("Firewalls: ${diffParams.snakeFirewallCount}", style = MaterialTheme.typography.labelMedium, color = CoralMistake)
                                Text("Target Cores: ${diffParams.snakeTargetCores}", style = MaterialTheme.typography.labelMedium, color = EmeraldTelemetry)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Latest Calibration Log: ${profile.lastAdjustmentReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Learned Neural Heuristic Weights Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Heuristic Weights",
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EVOLVED NEURAL HEURISTIC WEIGHTS",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    HeuristicBar("Aggression & Immediate Reward", profile.aggressionWeight.toInt(), ElectricCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    HeuristicBar("Defensive Space & Survival", profile.defenseWeight.toInt(), EmeraldTelemetry)
                    Spacer(modifier = Modifier.height(8.dp))
                    HeuristicBar("Multi-Turn Lookahead & Synergy", profile.lookaheadWeight.toInt(), PurpleNeural)
                    Spacer(modifier = Modifier.height(8.dp))
                    HeuristicBar("Trap & Mistake Avoidance", profile.trapAwarenessWeight.toInt(), AmberWarning)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "MATCH EPISODE & ADAPTATION LOG (${gameEpisodes.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (gameEpisodes.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "No Episodes Yet",
                            tint = PurpleNeural,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Match Episodes Logged Yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Complete a match in the AI Arena to see post-match reflections, memory recall counts, and difficulty tier adjustments.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                gameEpisodes.forEach { ep ->
                    MatchEpisodeCard(episode = ep)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeuristicBar(
    label: String,
    weight: Int,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text("$weight / 100", style = MaterialTheme.typography.labelMedium, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (weight.coerceIn(0, 100)) / 100f },
            color = color,
            trackColor = ObsidianBackground,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun MatchEpisodeCard(episode: MatchEpisode) {
    val isWin = episode.outcome == "win"
    val accent = if (isWin) EmeraldTelemetry else CoralMistake

    Card(
        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${episode.outcome.uppercase()} • TIER ${episode.difficultyLevel} (${episode.finalScore} PTS)",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
                val deltaBadge = when {
                    episode.difficultyDelta > 0 -> "TIER +${episode.difficultyDelta} ↑"
                    episode.difficultyDelta < 0 -> "TIER ${episode.difficultyDelta} ↓"
                    else -> "TIER STEADY"
                }
                Text(
                    text = deltaBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Turns: ${episode.movesCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Memories Queried: ${episode.memoriesRecalled}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )
                Text(
                    text = "Mistakes Avoided: ${episode.mistakesAvoided}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldTelemetry
                )
                Text(
                    text = "New Mistakes: ${episode.mistakesCommitted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoralMistake
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = episode.aiReflection,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
