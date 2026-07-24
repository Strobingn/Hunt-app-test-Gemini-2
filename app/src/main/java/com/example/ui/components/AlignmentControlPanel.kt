package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ColorRamp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AlignmentState
import kotlin.math.roundToInt

@Composable
fun AlignmentControlPanel(
    alignmentState: AlignmentState,
    onNudge: (Float, Float) -> Unit, // (deltaNorth, deltaEast)
    onSetNudgeStep: (Float) -> Unit,
    onUpdateScale: (Float) -> Unit,
    onUpdateRotation: (Float) -> Unit,
    onUpdateOpacity: (Float) -> Unit,
    onSetColorRamp: (ColorRamp) -> Unit,
    onSetElevationFilter: (Float, Float) -> Unit,
    onSetSplitCurtainRatio: (Float) -> Unit,
    onToggleLock: () -> Unit,
    onSnapToMapCenter: () -> Unit,
    onActivateCrosshair: () -> Unit,
    onSavePresetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Position/Scale, 1 = Display/Curtain, 2 = Elevation
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(containerColor = TacticalDarkSurface.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Panel Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (alignmentState.isLocked) Icons.Default.Lock else Icons.Default.Tune,
                        contentDescription = "Alignment Controls",
                        tint = if (alignmentState.isLocked) ForestSageLight else HunterAmber
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "LAZ Position & Scale Alignment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (alignmentState.isLocked) "POSITION LOCKED" else "Adjusting local coordinates to Google Map GPS",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (alignmentState.isLocked) ForestSageLight else TextSecondaryLight
                        )
                    }
                }

                Row {
                    // Lock Toggle Button
                    IconButton(
                        onClick = onToggleLock,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (alignmentState.isLocked) ForestSageContainer else Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = if (alignmentState.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock Position",
                            tint = if (alignmentState.isLocked) ForestSagePrimary else Color.White
                        )
                    }

                    // Save Preset Button
                    IconButton(onClick = onSavePresetClick) {
                        Icon(Icons.Default.Save, contentDescription = "Save Alignment", tint = HunterAmber)
                    }

                    // Collapse / Expand
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Toggle Expand",
                            tint = Color.White
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(8.dp))

                    // Tab Selector Bar
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = TacticalSurfaceVariant,
                        contentColor = ForestSageLight,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("Position & Rotation", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("Opacity & Curtain", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            text = { Text("Elevation Ramp", fontSize = 11.sp) }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    when (activeTab) {
                        0 -> PositionTabContent(
                            alignmentState = alignmentState,
                            onNudge = onNudge,
                            onSetNudgeStep = onSetNudgeStep,
                            onUpdateScale = onUpdateScale,
                            onUpdateRotation = onUpdateRotation,
                            onSnapToMapCenter = onSnapToMapCenter,
                            onActivateCrosshair = onActivateCrosshair
                        )
                        1 -> OpacityCurtainTabContent(
                            alignmentState = alignmentState,
                            onUpdateOpacity = onUpdateOpacity,
                            onSetSplitCurtainRatio = onSetSplitCurtainRatio
                        )
                        2 -> ElevationRampTabContent(
                            alignmentState = alignmentState,
                            onSetColorRamp = onSetColorRamp,
                            onSetElevationFilter = onSetElevationFilter
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PositionTabContent(
    alignmentState: AlignmentState,
    onNudge: (Float, Float) -> Unit,
    onSetNudgeStep: (Float) -> Unit,
    onUpdateScale: (Float) -> Unit,
    onUpdateRotation: (Float) -> Unit,
    onSnapToMapCenter: () -> Unit,
    onActivateCrosshair: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nudge Step Size Chips
            Text("Step:", style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(0.1f, 1.0f, 10.0f, 50.0f).forEach { step ->
                    FilterChip(
                        selected = alignmentState.nudgeStepMeters == step,
                        onClick = { onSetNudgeStep(step) },
                        label = { Text("${step}m", fontSize = 10.sp) },
                        modifier = Modifier.height(26.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nudge D-Pad Controls
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .background(TacticalSurfaceVariant, shape = RoundedCornerShape(12.dp))
            ) {
                // North
                IconButton(
                    onClick = { onNudge(1f, 0f) },
                    enabled = !alignmentState.isLocked,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(32.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, "North", tint = HunterAmber)
                }
                // South
                IconButton(
                    onClick = { onNudge(-1f, 0f) },
                    enabled = !alignmentState.isLocked,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(32.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, "South", tint = HunterAmber)
                }
                // West
                IconButton(
                    onClick = { onNudge(0f, -1f) },
                    enabled = !alignmentState.isLocked,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(32.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, "West", tint = HunterAmber)
                }
                // East
                IconButton(
                    onClick = { onNudge(0f, 1f) },
                    enabled = !alignmentState.isLocked,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, "East", tint = HunterAmber)
                }
                // Center Crosshair
                IconButton(
                    onClick = onActivateCrosshair,
                    enabled = !alignmentState.isLocked,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.GpsFixed, "Drop Crosshair Anchor", tint = Color.White)
                }
            }

            Spacer(Modifier.width(12.dp))

            // Scale & Rotation Controls
            Column(modifier = Modifier.weight(1f)) {
                // Scale Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scale: ${"%.2f".format(alignmentState.scaleX)}x", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Row {
                        IconButton(
                            onClick = { onUpdateScale(alignmentState.scaleX - 0.05f) },
                            enabled = !alignmentState.isLocked,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Remove, "Zoom Out", tint = HunterAmber)
                        }
                        IconButton(
                            onClick = { onUpdateScale(alignmentState.scaleX + 0.05f) },
                            enabled = !alignmentState.isLocked,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Add, "Zoom In", tint = HunterAmber)
                        }
                    }
                }
                Slider(
                    value = alignmentState.scaleX,
                    onValueChange = onUpdateScale,
                    valueRange = 0.2f..5.0f,
                    enabled = !alignmentState.isLocked,
                    colors = SliderDefaults.colors(thumbColor = HunterAmber, activeTrackColor = HunterAmber)
                )

                // Rotation Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Rotation: ${alignmentState.rotationDegrees.roundToInt()}°", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Button(
                        onClick = { onUpdateRotation(alignmentState.rotationDegrees + 90f) },
                        enabled = !alignmentState.isLocked,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("+90°", fontSize = 9.sp)
                    }
                }
                Slider(
                    value = alignmentState.rotationDegrees,
                    onValueChange = onUpdateRotation,
                    valueRange = 0f..360f,
                    enabled = !alignmentState.isLocked,
                    colors = SliderDefaults.colors(thumbColor = TopoCyan, activeTrackColor = TopoCyan)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Center GPS Quick Snap
        Button(
            onClick = onSnapToMapCenter,
            enabled = !alignmentState.isLocked,
            colors = ButtonDefaults.buttonColors(containerColor = TacticalSurfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CenterFocusStrong, "Snap", modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Snap LAZ Anchor to Current Map Center", fontSize = 11.sp, color = Color.White)
        }
    }
}

@Composable
private fun OpacityCurtainTabContent(
    alignmentState: AlignmentState,
    onUpdateOpacity: (Float) -> Unit,
    onSetSplitCurtainRatio: (Float) -> Unit
) {
    Column {
        // Opacity
        Text("Overlay Opacity: ${(alignmentState.opacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
        Slider(
            value = alignmentState.opacity,
            onValueChange = onUpdateOpacity,
            valueRange = 0.05f..1.0f,
            colors = SliderDefaults.colors(thumbColor = ForestSageLight, activeTrackColor = ForestSagePrimary)
        )

        Spacer(Modifier.height(8.dp))

        // Split Curtain Wipe Tool
        Text(
            text = "Split-Screen Wipe Inspection Curtain: ${(alignmentState.splitCurtainRatio * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
        Text(
            text = "Slide curtain to compare LAZ elevation contours directly with underlying Google satellite imagery",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryLight,
            fontSize = 10.sp
        )
        Slider(
            value = alignmentState.splitCurtainRatio,
            onValueChange = onSetSplitCurtainRatio,
            valueRange = 0.0f..1.0f,
            colors = SliderDefaults.colors(thumbColor = HunterAmber, activeTrackColor = HunterAmber)
        )
    }
}

@Composable
private fun ElevationRampTabContent(
    alignmentState: AlignmentState,
    onSetColorRamp: (ColorRamp) -> Unit,
    onSetElevationFilter: (Float, Float) -> Unit
) {
    Column {
        Text("Elevation Color Ramp Scheme:", style = MaterialTheme.typography.labelSmall, color = Color.White)
        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ColorRamp.entries.take(3).forEach { ramp ->
                FilterChip(
                    selected = alignmentState.colorRamp == ramp,
                    onClick = { onSetColorRamp(ramp) },
                    label = { Text(ramp.displayName, fontSize = 9.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ColorRamp.entries.drop(3).forEach { ramp ->
                FilterChip(
                    selected = alignmentState.colorRamp == ramp,
                    onClick = { onSetColorRamp(ramp) },
                    label = { Text(ramp.displayName, fontSize = 9.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Elevation Filter Range
        Text(
            text = "Height Cutoff Filter (Ground vs Vegetation): ${(alignmentState.minElevationFilter * 100).roundToInt()}% - ${(alignmentState.maxElevationFilter * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
        RangeSlider(
            value = alignmentState.minElevationFilter..alignmentState.maxElevationFilter,
            onValueChange = { range ->
                onSetElevationFilter(range.start, range.endInclusive)
            },
            valueRange = 0.0f..1.0f,
            colors = SliderDefaults.colors(thumbColor = TopoCyan, activeTrackColor = TopoCyan)
        )
    }
}
