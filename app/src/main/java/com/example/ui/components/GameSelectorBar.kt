package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.GameType
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldTelemetry
import com.example.ui.theme.PurpleNeural
import com.example.ui.theme.SlateSurface

/**
 * Flow-wrapped, fully readable game selector ensuring every game title is
 * unobstructed, reachable, and meets 48dp accessibility touch targets across
 * compact phones, foldables, and tablets.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameSelectorBar(
    selectedGame: GameType,
    onSelectGame: (GameType) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GameType.entries.forEach { game ->
            val isSelected = game == selectedGame
            val gameAccent = when (game) {
                GameType.TACTICAL_GRID -> ElectricCyan
                GameType.NEURAL_2048 -> EmeraldTelemetry
                GameType.CYBER_SNAKE -> PurpleNeural
            }
            val gameIcon = when (game) {
                GameType.TACTICAL_GRID -> Icons.Filled.GridOn
                GameType.NEURAL_2048 -> Icons.Filled.Grid4x4
                GameType.CYBER_SNAKE -> Icons.Filled.Route
            }

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) gameAccent.copy(alpha = 0.16f) else SlateSurface,
                animationSpec = tween(durationMillis = 200),
                label = "game_pill_bg_${game.id}"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) gameAccent.copy(alpha = 0.85f) else BorderSubtle,
                animationSpec = tween(durationMillis = 200),
                label = "game_pill_border_${game.id}"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) gameAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 200),
                label = "game_pill_fg_${game.id}"
            )

            Surface(
                selected = isSelected,
                onClick = { onSelectGame(game) },
                shape = RoundedCornerShape(14.dp),
                color = containerColor,
                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
                shadowElevation = if (isSelected) 4.dp else 0.dp,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        this.role = Role.Tab
                        this.selected = isSelected
                        this.contentDescription = "${game.title}: ${game.subtitle}"
                    }
                    .testTag("select_game_${game.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = gameIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
