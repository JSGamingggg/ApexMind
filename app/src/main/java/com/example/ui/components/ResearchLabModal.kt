package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.GameType
import com.example.ui.ResearchModalState
import com.example.ui.theme.CoralMistake
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedCardSurface
import com.example.ui.theme.EmeraldTelemetry
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SlateSurface

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResearchLabModal(
    selectedGame: GameType,
    modalState: ResearchModalState,
    onFetchStrategy: (gameTitle: String, externalSource: String, sourceType: String) -> Unit,
    onOpenFullLab: () -> Unit,
    onDismiss: () -> Unit
) {
    var gameTitle by rememberSaveable(selectedGame.id) { mutableStateOf(selectedGame.title) }
    var externalSource by rememberSaveable(selectedGame.id) { mutableStateOf("") }
    var selectedSourceType by rememberSaveable(selectedGame.id) { mutableStateOf("wiki") }

    val isFetching = modalState is ResearchModalState.Fetching

    Dialog(
        onDismissRequest = { if (!isFetching) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .animateContentSize(
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                )
                .testTag("research_lab_modal")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = stringResource(R.string.research_modal_title),
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.research_modal_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Target Arena Profile: ${selectedGame.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isFetching,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("research_modal_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.research_modal_close_desc),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Enter a game title and optionally an external URL or source query. Gemini fetches external strategy material and extracts checkable objectives into Firestore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Game Title Input
                OutlinedTextField(
                    value = gameTitle,
                    onValueChange = { gameTitle = it },
                    label = { Text(stringResource(R.string.research_modal_game_title_label)) },
                    placeholder = { Text(stringResource(R.string.research_modal_game_title_placeholder)) },
                    singleLine = true,
                    enabled = !isFetching,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("research_modal_game_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // External Source Category Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "wiki" to "Game Wiki",
                        "strategy_guide" to "Strategy Guide",
                        "video_transcript" to "Video Transcript",
                        "custom" to "Web / Custom"
                    ).forEach { (typeKey, label) ->
                        FilterChip(
                            selected = selectedSourceType == typeKey,
                            onClick = { if (!isFetching) selectedSourceType = typeKey },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                selectedLabelColor = ElectricCyan
                            ),
                            modifier = Modifier.testTag("research_modal_type_$typeKey")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Optional External URL or Query Input
                OutlinedTextField(
                    value = externalSource,
                    onValueChange = { externalSource = it },
                    label = { Text(stringResource(R.string.research_modal_source_label)) },
                    placeholder = { Text(stringResource(R.string.research_modal_source_placeholder)) },
                    singleLine = true,
                    enabled = !isFetching,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Language,
                            contentDescription = "External source",
                            tint = ElectricCyan
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("research_modal_source_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Fetch via Gemini Button
                Button(
                    onClick = {
                        onFetchStrategy(gameTitle, externalSource, selectedSourceType)
                    },
                    enabled = !isFetching && gameTitle.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF001F26)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("research_modal_fetch_button")
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF001F26),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.research_modal_fetching),
                            style = MaterialTheme.typography.labelLarge
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = stringResource(R.string.research_modal_fetch_button),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.research_modal_fetch_button),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Animated Status / Extracted Objectives Preview
                AnimatedContent(
                    targetState = modalState,
                    transitionSpec = {
                        (fadeIn(tween(220)) + expandVertically(
                            animationSpec = tween(240, easing = FastOutSlowInEasing),
                            expandFrom = Alignment.Top
                        )) togetherWith (fadeOut(tween(160)) + shrinkVertically(
                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                            shrinkTowards = Alignment.Top
                        ))
                    },
                    label = "research_modal_state_transition"
                ) { state ->
                    when (state) {
                        ResearchModalState.Idle -> Spacer(modifier = Modifier.height(4.dp))

                        is ResearchModalState.Fetching -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElevatedCardSurface)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = state.statusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ElectricCyan
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = ElectricCyan,
                                    trackColor = ObsidianBackground
                                )
                            }
                        }

                        is ResearchModalState.Error -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CoralMistake.copy(alpha = 0.14f))
                                    .border(1.dp, CoralMistake.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                                    .testTag("research_modal_error_banner"),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Research fetch error",
                                    tint = CoralMistake,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoralMistake
                                )
                            }
                        }

                        is ResearchModalState.Success -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(EmeraldTelemetry.copy(alpha = 0.10f))
                                    .border(1.dp, EmeraldTelemetry.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                                    .testTag("research_modal_objectives_preview")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Objectives fetched",
                                        tint = EmeraldTelemetry,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (state.fetchedFromWeb) {
                                            "WEB SOURCE + GEMINI // ${state.sourceTitle}"
                                        } else {
                                            "GEMINI STRATEGY // ${state.sourceTitle}"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EmeraldTelemetry,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = state.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "EXTRACTED OBJECTIVES (${state.extractedObjectives.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                state.extractedObjectives.forEach { obj ->
                                    Text(
                                        text = "• $obj",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldTelemetry,
                                        contentColor = Color(0xFF00210D)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("research_modal_done_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.research_modal_done_button),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved directly to Firestore research_docs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            onDismiss()
                            onOpenFullLab()
                        },
                        enabled = !isFetching,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("research_modal_full_lab_button")
                    ) {
                        Text(
                            text = stringResource(R.string.research_modal_open_full_lab),
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                    }
                }
            }
        }
    }
}
