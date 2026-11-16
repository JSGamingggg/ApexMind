package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GameType
import com.example.data.StateMemory
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
fun MemoryVaultScreen(
    viewModel: ApexMindViewModel
) {
    val stateMemories by viewModel.stateMemories.collectAsStateWithLifecycle()
    val selectedGame by viewModel.selectedGame.collectAsStateWithLifecycle()

    var filterGameId by remember { mutableStateOf("ALL") }
    var filterOutcome by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }

    var newSig by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newAction by remember { mutableStateOf("") }
    var newOutcome by remember { mutableStateOf("mistake") }
    var newLesson by remember { mutableStateOf("") }

    val filteredMemories = stateMemories.filter { mem ->
        val matchesGame = filterGameId == "ALL" || mem.gameId == filterGameId
        val matchesOutcome = when (filterOutcome) {
            "WINS" -> mem.outcome == "win" || mem.outcome == "optimal"
            "MISTAKES" -> mem.outcome == "mistake" || mem.outcome == "loss"
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
            mem.stateSignature.contains(searchQuery, ignoreCase = true) ||
            mem.stateDescription.contains(searchQuery, ignoreCase = true) ||
            mem.actionTaken.contains(searchQuery, ignoreCase = true) ||
            mem.lessonLearned.contains(searchQuery, ignoreCase = true)

        matchesGame && matchesOutcome && matchesSearch
    }

    val totalRecalls = stateMemories.sumOf { it.timesRecalled }
    val mistakesStored = stateMemories.count { it.outcome == "mistake" || it.outcome == "loss" }
    val optimalStored = stateMemories.count { it.outcome == "win" || it.outcome == "optimal" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 680.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EPISODIC STATE-ACTION MEMORY VAULT",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Retained game states, actions taken, score deltas, and learned rules queried by the AI during live gameplay.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showAddForm = !showAddForm },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("toggle_add_memory_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Inject Memory Rule",
                        tint = ElectricCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Rule", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary KPI Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VaultStatCard(
                    label = "TOTAL",
                    value = "${stateMemories.size}",
                    accent = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
                VaultStatCard(
                    label = "OPTIMAL",
                    value = "$optimalStored",
                    accent = EmeraldTelemetry,
                    modifier = Modifier.weight(1f)
                )
                VaultStatCard(
                    label = "VETOES",
                    value = "$mistakesStored",
                    accent = CoralMistake,
                    modifier = Modifier.weight(1f)
                )
                VaultStatCard(
                    label = "RECALLS",
                    value = "$totalRecalls",
                    accent = PurpleNeural,
                    modifier = Modifier.weight(1f)
                )
            }

            // Optional Manual Memory Rule Injection Form
            AnimatedVisibility(
                visible = showAddForm,
                enter = fadeIn(tween(220)) + expandVertically(tween(240, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(160)) + shrinkVertically(tween(180, easing = FastOutSlowInEasing))
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "INJECT STATE-ACTION MEMORY FOR ${selectedGame.title.uppercase()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newSig,
                            onValueChange = { newSig = it },
                            label = { Text("State Signature (e.g., N2048_TL_CORNER_MAX128)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("memory_sig_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newDesc,
                            onValueChange = { newDesc = it },
                            label = { Text("Board State Description") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("memory_desc_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newAction,
                                onValueChange = { newAction = it },
                                label = { Text("Action (e.g., LEFT, SECTOR_2_2)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("memory_action_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = newOutcome == "optimal",
                                onClick = { newOutcome = "optimal" },
                                label = { Text("Optimal (+80 Q-Boost)") }
                            )
                            FilterChip(
                                selected = newOutcome == "mistake",
                                onClick = { newOutcome = "mistake" },
                                label = { Text("Mistake (-80 Veto Guard)") }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newLesson,
                            onValueChange = { newLesson = it },
                            label = { Text("Lesson Learned / Decision Rule") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("memory_lesson_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (newSig.isNotBlank() && newAction.isNotBlank()) {
                                    viewModel.addManualStateMemory(
                                        game = selectedGame,
                                        stateSignature = newSig,
                                        stateDescription = newDesc.ifBlank { "Custom operator state" },
                                        actionTaken = newAction,
                                        outcome = newOutcome,
                                        qValueScore = if (newOutcome == "mistake") -80 else 80,
                                        lessonLearned = newLesson.ifBlank { "Operator-encoded state-action rule." }
                                    )
                                    newSig = ""
                                    newDesc = ""
                                    newAction = ""
                                    newLesson = ""
                                    showAddForm = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF001F26)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_memory_button")
                        ) {
                            Text("SAVE TO STATE MEMORY BANK", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search & Filter Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search State Memories"
                    )
                },
                label = { Text("Query by state key, action, or lesson...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("memory_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = filterGameId == "ALL",
                    onClick = { filterGameId = "ALL" },
                    label = { Text("All Games") }
                )
                GameType.entries.forEach { g ->
                    FilterChip(
                        selected = filterGameId == g.id,
                        onClick = { filterGameId = g.id },
                        label = { Text(g.title) }
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "ALL" to "All Outcomes",
                    "WINS" to "Winning Moves",
                    "MISTAKES" to "Mistakes & Vetoes"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = filterOutcome == key,
                        onClick = { filterOutcome = key },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberWarning.copy(alpha = 0.22f),
                            selectedLabelColor = AmberWarning
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedContent(
                targetState = filteredMemories.isEmpty(),
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                label = "memory_vault_list_transition"
            ) { isEmpty ->
                if (isEmpty) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = "Empty Memory Filter",
                                tint = ElectricCyan,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No State-Action Memories Match Filter",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Run matches in the AI Arena or synthesize Pre-Game Research to populate the episodic memory bank.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredMemories.forEach { memory ->
                            StateMemoryCard(
                                memory = memory,
                                onDelete = { viewModel.deleteMemory(memory.id) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VaultStatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SlateSurface)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun StateMemoryCard(
    memory: StateMemory,
    onDelete: () -> Unit
) {
    val isNegative = memory.outcome == "mistake" || memory.outcome == "loss" || memory.qValueScore < 0
    val accent = if (isNegative) CoralMistake else EmeraldTelemetry
    val gameTitle = GameType.fromId(memory.gameId).title

    Card(
        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isNegative) "MISTAKE VETO" else "OPTIMAL PATTERN",
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianBackground,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accent)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                    Text(
                        text = "$gameTitle • T${memory.difficultyLevel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Memory",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "State Key: ${memory.stateSignature}",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricCyan
            )
            Text(
                text = memory.stateDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianBackground)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Action: ${memory.actionTaken}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                Text(
                    text = "Delta: ${if (memory.scoreDelta >= 0) "+${memory.scoreDelta}" else "${memory.scoreDelta}"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
                Text(
                    text = "Q-Utility: ${if (memory.qValueScore >= 0) "+${memory.qValueScore}" else "${memory.qValueScore}"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
                Text(
                    text = "Recalled: ${memory.timesRecalled}x",
                    style = MaterialTheme.typography.labelMedium,
                    color = PurpleNeural
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Retained Lesson: ${memory.lessonLearned}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
