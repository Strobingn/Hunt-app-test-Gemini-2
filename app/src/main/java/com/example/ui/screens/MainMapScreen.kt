package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HuntingWaypoint
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.HuntMapViewModel
import com.example.ui.viewmodel.MapType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMapScreen(
    viewModel: HuntMapViewModel
) {
    val mapState by viewModel.mapState.collectAsStateWithLifecycle()
    val alignmentState by viewModel.alignmentState.collectAsStateWithLifecycle()
    val savedOverlays by viewModel.savedOverlays.collectAsStateWithLifecycle()
    val waypoints by viewModel.waypoints.collectAsStateWithLifecycle()
    val isCrosshairActive by viewModel.isCrosshairPickerActive.collectAsStateWithLifecycle()
    val oracleBackendUrl by viewModel.oracleBackendUrl.collectAsStateWithLifecycle()
    val isAnalyzingAi by viewModel.isAnalyzingAi.collectAsStateWithLifecycle()
    val aiAnalysisResult by viewModel.aiAnalysisResult.collectAsStateWithLifecycle()

    // ML State flows
    val showCorridors by viewModel.showCorridors.collectAsStateWithLifecycle()
    val showThermals by viewModel.showThermals.collectAsStateWithLifecycle()
    val showFeatureHeatmap by viewModel.showFeatureHeatmap.collectAsStateWithLifecycle()
    val timeOfDay by viewModel.timeOfDay.collectAsStateWithLifecycle()
    val corridors by viewModel.corridors.collectAsStateWithLifecycle()
    val thermalVectors by viewModel.thermalVectors.collectAsStateWithLifecycle()
    val scentPlumes by viewModel.scentPlumes.collectAsStateWithLifecycle()
    val topographicGrid by viewModel.topographicGrid.collectAsStateWithLifecycle()

    var showDatasetPickerDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showAddWaypointDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var showTrailCamDialog by remember { mutableStateOf(false) }
    var wayPointTargetLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var showMapTypeMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Terrain,
                                contentDescription = null,
                                tint = LiDARGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Hunt Align",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = alignmentState.lazDataset?.name ?: "No LAZ File Loaded",
                            fontSize = 11.sp,
                            color = ForestSageLight
                        )
                    }
                },
                actions = {
                    // Map Type Selector
                    Box {
                        IconButton(onClick = { showMapTypeMenu = true }) {
                            Icon(Icons.Default.Map, "Map Type", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMapTypeMenu,
                            onDismissRequest = { showMapTypeMenu = false },
                            modifier = Modifier.background(TacticalDarkSurface)
                        ) {
                            MapType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.label, color = Color.White) },
                                    onClick = {
                                        viewModel.setMapType(type)
                                        showMapTypeMenu = false
                                    },
                                    leadingIcon = {
                                        if (mapState.mapType == type) {
                                            Icon(Icons.Default.Check, null, tint = LiDARGreen)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // AI Terrain Assistant Button
                    IconButton(onClick = { showAiDialog = true }) {
                        Icon(Icons.Default.AutoAwesome, "AI Terrain Assistant", tint = LiDARGreen)
                    }

                    // Dataset / Preset Picker Button
                    IconButton(onClick = { showDatasetPickerDialog = true }) {
                        Icon(Icons.Default.FolderOpen, "Select Dataset", tint = HunterAmber)
                    }

                    // Add Waypoint Button
                    IconButton(onClick = {
                        wayPointTargetLocation = Pair(mapState.centerLat, mapState.centerLng)
                        showAddWaypointDialog = true
                    }) {
                        Icon(Icons.Default.AddLocation, "Add Waypoint", tint = LiDARGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TacticalDarkSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Interactive Map & LAZ LiDAR Overlay Engine
            MapCanvasOverlay(
                mapState = mapState,
                alignmentState = alignmentState,
                waypoints = waypoints,
                isCrosshairActive = isCrosshairActive,
                corridors = corridors,
                thermalVectors = thermalVectors,
                scentPlumes = scentPlumes,
                topographicGrid = topographicGrid,
                showCorridors = showCorridors,
                showThermals = showThermals,
                showFeatureHeatmap = showFeatureHeatmap,
                onMapPan = { dx, dy -> viewModel.panMapBy(dx, dy) },
                onNudgeOverlay = { n, e -> viewModel.nudgeOverlay(n, e) },
                onRotateOverlay = { deg -> viewModel.updateRotation(deg) },
                onScaleOverlay = { scale -> viewModel.updateScale(scale) },
                onWaypointClick = { wp -> viewModel.selectWaypointDetails(wp) },
                onCrosshairPointSelected = { lat, lng ->
                    viewModel.setCenterLat(lat)
                    viewModel.setCenterLng(lng)
                    viewModel.setCrosshairPickerActive(false)
                }
            )

            // ML Control Panel Bar Overlay at Top
            MlControlPanelBar(
                showCorridors = showCorridors,
                showThermals = showThermals,
                showFeatureHeatmap = showFeatureHeatmap,
                timeOfDay = timeOfDay,
                onToggleCorridors = { viewModel.toggleCorridors() },
                onToggleThermals = { viewModel.toggleThermals() },
                onToggleFeatureHeatmap = { viewModel.toggleFeatureHeatmap() },
                onTimeOfDayChange = { tod -> viewModel.setTimeOfDay(tod) },
                onOpenTrailCamAi = { showTrailCamDialog = true },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

            // Bottom Alignment Control Panel
            AlignmentControlPanel(
                alignmentState = alignmentState,
                onNudge = { n, e -> viewModel.nudgeOverlay(n, e) },
                onSetNudgeStep = { step -> viewModel.setNudgeStep(step) },
                onUpdateScale = { scale -> viewModel.updateScale(scale) },
                onUpdateRotation = { deg -> viewModel.updateRotation(deg) },
                onUpdateOpacity = { opacity -> viewModel.updateOpacity(opacity) },
                onSetColorRamp = { ramp -> viewModel.setColorRamp(ramp) },
                onSetElevationFilter = { min, max -> viewModel.setElevationFilter(min, max) },
                onSetSplitCurtainRatio = { ratio -> viewModel.setSplitCurtainRatio(ratio) },
                onToggleLock = { viewModel.toggleLock() },
                onSnapToMapCenter = { viewModel.snapAlignmentToMapCenter() },
                onActivateCrosshair = { viewModel.setCrosshairPickerActive(!isCrosshairActive) },
                onSavePresetClick = { showSavePresetDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            )
        }
    }

    // Dialogs
    if (showDatasetPickerDialog) {
        DatasetAndPresetPickerDialog(
            sampleDatasets = viewModel.sampleDatasets,
            savedOverlays = savedOverlays,
            activeDatasetId = alignmentState.lazDataset?.id,
            onSelectDataset = { dataset -> viewModel.selectDataset(dataset) },
            onSelectSavedOverlay = { saved -> viewModel.loadSavedOverlay(saved) },
            onDeleteSavedOverlay = { saved -> viewModel.deleteSavedOverlay(saved) },
            onDismiss = { showDatasetPickerDialog = false }
        )
    }

    if (showSavePresetDialog) {
        SaveOverlayDialog(
            defaultName = "${alignmentState.lazDataset?.name ?: "Overlay"} Alignment",
            onDismiss = { showSavePresetDialog = false },
            onConfirm = { name ->
                viewModel.saveCurrentOverlayPreset(name)
                showSavePresetDialog = false
            }
        )
    }

    if (showAddWaypointDialog && wayPointTargetLocation != null) {
        val loc = wayPointTargetLocation!!
        AddWaypointDialog(
            initialLat = loc.first,
            initialLng = loc.second,
            onDismiss = { showAddWaypointDialog = false },
            onConfirm = { title, note, type, lat, lng, elev ->
                viewModel.addWaypoint(title, note, type, lat, lng, elev)
                showAddWaypointDialog = false
            }
        )
    }

    if (showAiDialog) {
        AiTerrainDialog(
            dataset = alignmentState.lazDataset,
            centerLat = mapState.centerLat,
            centerLng = mapState.centerLng,
            waypoints = waypoints,
            oracleBackendUrl = oracleBackendUrl,
            isAnalyzing = isAnalyzingAi,
            analysisResult = aiAnalysisResult,
            onUpdateBackendUrl = { url -> viewModel.setOracleBackendUrl(url) },
            onRunAnalysis = { prompt -> viewModel.runAiTerrainAnalysis(prompt) },
            onDismiss = { showAiDialog = false }
        )
    }

    if (showTrailCamDialog) {
        AiTrailCamDialog(
            onDismiss = { showTrailCamDialog = false }
        )
    }
}
