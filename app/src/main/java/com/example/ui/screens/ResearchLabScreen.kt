package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.CuratedResearchLibrary
import com.example.data.GameType
import com.example.data.ResearchDoc
import com.example.ui.AiTaskState
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
fun ResearchLabScreen(
    viewModel: ApexMindViewModel,
    onLaunchGameAfterResearch: () -> Unit
) {
    val selectedGame by viewModel.selectedGame.collectAsStateWithLifecycle()
    val researchDocs by viewModel.researchDocs.collectAsStateWithLifecycle()
    val aiTaskState by viewModel.aiTaskState.collectAsStateWithLifecycle()

    val curatedForGame = remember(selectedGame) {
        CuratedResearchLibrary.getSourcesForGame(selectedGame)
    }

    var selectedSourceType by remember { mutableStateOf("wiki") }
    var customTitle by remember { mutableStateOf("") }
    var customContent by remember { mutableStateOf("") }

    LaunchedEffect(selectedGame) {
        curatedForGame.firstOrNull()?.let { first ->
            selectedSourceType = first.sourceType
            customTitle = first.title
            customContent = first.contentExcerpt
        }
    }

    val gameResearchDocs = researchDocs.filter { it.gameId == selectedGame.id }
    val isRunning = aiTaskState is AiTaskState.Running

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 680.dp)) {
            Text(
                text = "PRE-GAME STRATEGY RESEARCH MODULE",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Feed game wikis, strategy guides, or video transcripts to Gemini before playing. Extracted objectives and rules permanently tune the AI's heuristic weights and state memory.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Target Game Selector
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

            // Curated External Knowledge Dossiers (Wikis, Strategy Guides, Video Transcripts)
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. SELECT CURATED WIKI, GUIDE OR TRANSCRIPT SOURCE",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    curatedForGame.forEach { source ->
                        val isSelected = customTitle == source.title
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) ElectricCyan.copy(alpha = 0.14f) else ObsidianBackground)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) ElectricCyan else BorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedSourceType = source.sourceType
                                    customTitle = source.title
                                    customContent = source.contentExcerpt
                                }
                                .padding(12.dp)
                                .testTag("curated_source_${source.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = source.badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberWarning
                                )
                                Text(
                                    text = source.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected Source",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "2. REVIEW OR PASTE CUSTOM EXTERNAL RESEARCH MATERIAL",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            "wiki" to "Game Wiki",
                            "strategy_guide" to "Strategy Guide",
                            "video_transcript" to "Video Transcript",
                            "custom" to "Custom Notes"
                        ).forEach { (typeKey, label) ->
                            FilterChip(
                                selected = selectedSourceType == typeKey,
                                onClick = { selectedSourceType = typeKey },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Source Title (Wiki / Guide / Video Name)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("research_title_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customContent,
                        onValueChange = { customContent = it },
                        label = { Text("Strategy Guide, Rules Wiki, or Transcript Content") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("research_content_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.runPreGameResearch(
                                    game = selectedGame,
                                    sourceTitle = customTitle,
                                    sourceType = selectedSourceType,
                                    rawContent = customContent
                                )
                            },
                            enabled = !isRunning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF001F26)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp)
                                .testTag("synthesize_research_button")
                        ) {
                            if (isRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color(0xFF001F26),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("RESEARCHING...", style = MaterialTheme.typography.labelLarge)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Synthesize Strategy"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SYNTHESIZE & LEARN", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        OutlinedButton(
                            onClick = onLaunchGameAfterResearch,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("play_after_research_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Now",
                                tint = EmeraldTelemetry
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PLAY GAME", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    when (val task = aiTaskState) {
                        is AiTaskState.Error -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = task.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = CoralMistake
                            )
                        }
                        is AiTaskState.Success -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = task.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldTelemetry
                            )
                        }
                        else -> Unit
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "RETAINED RESEARCH DOSSIERS FOR ${selectedGame.title.uppercase()} (${gameResearchDocs.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (gameResearchDocs.isEmpty()) {
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
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "No research yet",
                            tint = AmberWarning,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Pre-Game Research Synthesized Yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Select one of the curated game wikis or strategy guides above and tap SYNTHESIZE & LEARN so the AI studies the game before playing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                gameResearchDocs.forEach { doc ->
                    ResearchDossierCard(doc = doc)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResearchDossierCard(doc: ResearchDoc) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = doc.sourceType.uppercase().replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberWarning
                )
                Text(
                    text = "Boosts: AGG ${formatSigned(doc.aggressionBoost)} | DEF ${formatSigned(doc.defenseBoost)} | LOOK ${formatSigned(doc.lookaheadBoost)} | TRAP ${formatSigned(doc.trapAwarenessBoost)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldTelemetry
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = doc.sourceTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = doc.synthesizedSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (doc.keyObjectives.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "CORE OBJECTIVES",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
                doc.keyObjectives.forEach { obj ->
                    Text(
                        text = "• $obj",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (doc.tacticalRules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "LEARNED TACTICAL DECISION RULES",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldTelemetry
                )
                doc.tacticalRules.forEach { rule ->
                    Text(
                        text = "✓ $rule",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

private fun formatSigned(value: Long): String = if (value >= 0) "+$value" else "$value"
