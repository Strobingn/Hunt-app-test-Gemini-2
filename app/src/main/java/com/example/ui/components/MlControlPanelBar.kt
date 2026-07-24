package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ml.ThermalScentSimulator
import com.example.ui.theme.*

@Composable
fun MlControlPanelBar(
    showCorridors: Boolean,
    showThermals: Boolean,
    showFeatureHeatmap: Boolean,
    timeOfDay: ThermalScentSimulator.TimeOfDay,
    onToggleCorridors: () -> Unit,
    onToggleThermals: () -> Unit,
    onToggleFeatureHeatmap: () -> Unit,
    onTimeOfDayChange: (ThermalScentSimulator.TimeOfDay) -> Unit,
    onOpenTrailCamAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = TacticalDarkSurface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Corridors Toggle Chip
            FilterChip(
                selected = showCorridors,
                onClick = onToggleCorridors,
                label = { Text("Corridors", fontSize = 10.sp) },
                leadingIcon = { Icon(Icons.Default.Pets, null, modifier = Modifier.size(12.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HunterAmber,
                    selectedLabelColor = Color.Black
                )
            )

            // Thermals Toggle Chip
            FilterChip(
                selected = showThermals,
                onClick = onToggleThermals,
                label = { Text("Thermals", fontSize = 10.sp) },
                leadingIcon = { Icon(Icons.Default.Air, null, modifier = Modifier.size(12.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TopoCyan,
                    selectedLabelColor = Color.Black
                )
            )

            // Features Toggle Chip
            FilterChip(
                selected = showFeatureHeatmap,
                onClick = onToggleFeatureHeatmap,
                label = { Text("Saddles", fontSize = 10.sp) },
                leadingIcon = { Icon(Icons.Default.Landscape, null, modifier = Modifier.size(12.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LiDARGreen,
                    selectedLabelColor = Color.Black
                )
            )

            // Trail Cam AI Button
            IconButton(
                onClick = onOpenTrailCamAi,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Trail Cam AI",
                    tint = HunterAmber,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
