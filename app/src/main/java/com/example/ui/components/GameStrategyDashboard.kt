package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.GameProfile
import com.example.data.GameType
import com.example.data.ResearchDoc
import com.example.engine.AdaptiveDifficultyEngine
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedCardSurface
import com.example.ui.theme.EmeraldTelemetry
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SlateSurface

private const val COMPACT_OBJECTIVES_LIMIT = 3
private const val DASHBOARD_ENTER_MS = 240
private const val DASHBOARD_EXIT_MS = 180

@Composable
fun GameStrategyDashboard(
    selectedGame: GameType,
    profile: GameProfile,
    researchDocs: List<ResearchDoc>,
    onToggleObjective: (String) -> Unit,
    onNavigateToResearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gameDocs = remember(researchDocs, selectedGame.id) {
        researchDocs.filter { it.gameId == selectedGame.id }
    }
    val strategicObjectives = remember(gameDocs) {
        gameDocs
            .flatMap { it.keyObjectives }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
    val completedSet = remember(profile.completedObjectives) {
        profile.completedObjectives.toSet()
    }
    val completedCount = remember(strategicObjectives, completedSet) {
        strategicObjectives.count { it in completedSet }
    }
    val diffParams = remember(profile.difficultyLevel) {
        AdaptiveDifficultyEngine.getParametersForLevel(profile.difficultyLevel.toInt())
    }
    val activeTargetText = remember(selectedGame, diffParams) {
        when (selectedGame) {
            GameType.TACTICAL_GRID ->
                "Outscore Tactical Bot in 14 turns (+${diffParams.tacticalTargetLead} pt target lead, ${diffParams.tacticalTrapCount} EMP traps)."
            GameType.NEURAL_2048 ->
                "Synthesize the ${diffParams.targetTile2048} milestone tile using corner-locking and monotonic merges."
            GameType.CYBER_SNAKE ->
                "Harvest ${diffParams.snakeTargetCores} data cores while evading ${diffParams.snakeFirewallCount} dynamic firewalls."
        }
    }

    var isExpanded by rememberSaveable(selectedGame.id) { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, BorderSubtle),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = DASHBOARD_ENTER_MS,
                    easing = FastOutSlowInEasing
                )
            )
            .testTag("game_strategy_dashboard")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row: Mission Target label + Tier / Win-Rate Goal pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.TrackChanges,
                        contentDescription = stringResource(R.string.strategy_dashboard_label),
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${stringResource(R.string.strategy_dashboard_label)} // ${selectedGame.title.uppercase()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBackground)
                        .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "TIER ${profile.difficultyLevel} · ${profile.targetWinRate}% WIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Current Game Target Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElevatedCardSurface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = "Current game target",
                    tint = EmeraldTelemetry,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(16.dp)
                )
                Text(
                    text = activeTargetText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("game_strategy_target_text")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedContent(
                targetState = strategicObjectives.isNotEmpty(),
                transitionSpec = {
                    (fadeIn(animationSpec = tween(DASHBOARD_ENTER_MS)) +
                        expandVertically(
                            animationSpec = tween(DASHBOARD_ENTER_MS, easing = FastOutSlowInEasing),
                            expandFrom = Alignment.Top
                        )) togetherWith
                        (fadeOut(animationSpec = tween(DASHBOARD_EXIT_MS)) +
                            shrinkVertically(
                                animationSpec = tween(DASHBOARD_EXIT_MS, easing = FastOutSlowInEasing),
                                shrinkTowards = Alignment.Top
                            ))
                },
                label = "strategy_objectives_presence"
            ) { hasObjectives ->
                if (!hasObjectives) {
                    // Empty state: show clear prompt + Open Research Lab button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmberWarning.copy(alpha = 0.10f))
                            .border(1.dp, AmberWarning.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.strategy_objectives_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = onNavigateToResearch,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = AmberWarning.copy(alpha = 0.22f),
                                contentColor = AmberWarning
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("dashboard_open_research_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = stringResource(R.string.strategy_open_research_lab),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.strategy_open_research_lab),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                } else {
                    Column {
                        // Objectives Progress Header
                        val totalCount = strategicObjectives.size.coerceAtLeast(1)
                        val allDone = completedCount == strategicObjectives.size
                        val animatedProgress by animateFloatAsState(
                            targetValue = completedCount.toFloat() / totalCount.toFloat(),
                            animationSpec = tween(
                                durationMillis = DASHBOARD_ENTER_MS,
                                easing = FastOutSlowInEasing
                            ),
                            label = "strategy_objectives_progress"
                        )
                        val progressAccent by animateColorAsState(
                            targetValue = if (allDone) EmeraldTelemetry else ElectricCyan,
                            animationSpec = tween(durationMillis = DASHBOARD_ENTER_MS),
                            label = "strategy_objectives_accent"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${stringResource(R.string.strategy_objectives_heading)} (${gameDocs.size} ${if (gameDocs.size == 1) "DOSSIER" else "DOSSIERS"})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$completedCount / ${strategicObjectives.size} COMPLETED",
                                style = MaterialTheme.typography.labelSmall,
                                color = progressAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("strategy_objectives_counter")
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            color = progressAccent,
                            trackColor = ObsidianBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Checkable Strategic Objectives List with subtle enter/exit transitions
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            strategicObjectives.forEachIndexed { index, objective ->
                                val isRowVisible = isExpanded || index < COMPACT_OBJECTIVES_LIMIT
                                key(objective) {
                                    val rowVisibilityState = remember(objective) {
                                        MutableTransitionState(false)
                                    }
                                    rowVisibilityState.targetState = isRowVisible

                                    AnimatedVisibility(
                                        visibleState = rowVisibilityState,
                                        enter = fadeIn(animationSpec = tween(210)) +
                                            expandVertically(
                                                animationSpec = tween(
                                                    durationMillis = 220,
                                                    easing = FastOutSlowInEasing
                                                ),
                                                expandFrom = Alignment.Top
                                            ),
                                        exit = fadeOut(animationSpec = tween(160)) +
                                            shrinkVertically(
                                                animationSpec = tween(
                                                    durationMillis = 180,
                                                    easing = FastOutSlowInEasing
                                                ),
                                                shrinkTowards = Alignment.Top
                                            )
                                    ) {
                                        val isChecked = objective in completedSet
                                        val rowBackground by animateColorAsState(
                                            targetValue = if (isChecked) {
                                                EmeraldTelemetry.copy(alpha = 0.08f)
                                            } else {
                                                ObsidianBackground.copy(alpha = 0.55f)
                                            },
                                            animationSpec = tween(durationMillis = 200),
                                            label = "objective_row_bg"
                                        )
                                        val rowTextColor by animateColorAsState(
                                            targetValue = if (isChecked) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            animationSpec = tween(durationMillis = 200),
                                            label = "objective_row_text"
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(rowBackground)
                                                .clickable { onToggleObjective(objective) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .testTag("strategy_objective_row_$index"),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { onToggleObjective(objective) },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = EmeraldTelemetry,
                                                    uncheckedColor = ElectricCyan.copy(alpha = 0.7f),
                                                    checkmarkColor = ObsidianBackground
                                                ),
                                                modifier = Modifier.testTag("strategy_objective_checkbox_$index")
                                            )
                                            Text(
                                                text = objective,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = rowTextColor,
                                                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(end = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (strategicObjectives.size > COMPACT_OBJECTIVES_LIMIT) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = { isExpanded = !isExpanded },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .heightIn(min = 48.dp)
                                    .testTag("strategy_dashboard_toggle_expand")
                            ) {
                                Text(
                                    text = if (isExpanded) {
                                        stringResource(R.string.strategy_show_less)
                                    } else {
                                        "${stringResource(R.string.strategy_show_more)} (${strategicObjectives.size})"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (isExpanded) {
                                        stringResource(R.string.strategy_show_less)
                                    } else {
                                        stringResource(R.string.strategy_show_more)
                                    },
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
