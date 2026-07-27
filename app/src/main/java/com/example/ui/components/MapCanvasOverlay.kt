package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ColorRamp
import com.example.data.model.HuntingWaypoint
import com.example.data.model.LazPoint
import com.example.data.service.MapTileProvider
import com.example.ml.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AlignmentState
import com.example.ui.viewmodel.MapState
import com.example.ui.viewmodel.MapType
import kotlin.math.*

@Composable
fun MapCanvasOverlay(
    mapState: MapState,
    alignmentState: AlignmentState,
    waypoints: List<HuntingWaypoint>,
    isCrosshairActive: Boolean,
    corridors: List<WildlifeCorridor> = emptyList(),
    thermalVectors: List<ThermalWindVector> = emptyList(),
    scentPlumes: List<ScentPlumeCone> = emptyList(),
    topographicGrid: Array<Array<TerrainCell>>? = null,
    showCorridors: Boolean = true,
    showThermals: Boolean = true,
    showFeatureHeatmap: Boolean = false,
    onMapPan: (Float, Float) -> Unit,
    onNudgeOverlay: (Float, Float) -> Unit,
    onRotateOverlay: (Float) -> Unit,
    onScaleOverlay: (Float) -> Unit,
    onWaypointClick: (HuntingWaypoint) -> Unit,
    onCrosshairPointSelected: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    var totalPanX by remember { mutableStateOf(0f) }
    var totalPanY by remember { mutableStateOf(0f) }

    // Read tileVersion at composable level to cleanly recompose when new tiles arrive
    val currentTileVersion by MapTileProvider.tileVersion.collectAsState()

    val dataset = alignmentState.lazDataset
    val cachedPointCloudBitmap: ImageBitmap? = remember(
        dataset?.id,
        alignmentState.colorRamp,
        alignmentState.minElevationFilter,
        alignmentState.maxElevationFilter,
        alignmentState.opacity
    ) {
        if (dataset == null || dataset.points.isEmpty()) null
        else {
            try {
                val bmpWidth = 450
                val bmpHeight = 450
                val bitmap = android.graphics.Bitmap.createBitmap(bmpWidth, bmpHeight, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

                val minZBound = dataset.minZ + (dataset.elevationRange * alignmentState.minElevationFilter)
                val maxZBound = dataset.minZ + (dataset.elevationRange * alignmentState.maxElevationFilter)

                for (pt in dataset.points) {
                    if (pt.z < minZBound || pt.z > maxZBound) continue

                    val normX = ((pt.x - dataset.minX) / dataset.width).coerceIn(0f, 1f)
                    val normY = ((dataset.maxY - pt.y) / dataset.height).coerceIn(0f, 1f)

                    val px = normX * bmpWidth
                    val py = normY * bmpHeight

                    val color = getPointColor(
                        pt = pt,
                        minZ = dataset.minZ,
                        maxZ = dataset.maxZ,
                        colorRamp = alignmentState.colorRamp,
                        opacity = alignmentState.opacity
                    )
                    paint.color = color.toArgb()

                    canvas.drawCircle(px, py, 3.5f, paint)
                }
                bitmap.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TacticalDarkBackground)
            .pointerInput(alignmentState.isLocked) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    if (!alignmentState.isLocked && (rotation != 0f || zoom != 1.0f)) {
                        if (abs(rotation) > 0.5f) {
                            onRotateOverlay(alignmentState.rotationDegrees + rotation)
                        }
                        if (abs(zoom - 1.0f) > 0.01f) {
                            onScaleOverlay(alignmentState.scaleX * zoom)
                        }
                    } else {
                        totalPanX += pan.x
                        totalPanY += pan.y
                        onMapPan(pan.x, pan.y)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val center = Offset(canvasWidth / 2f + mapState.mapOffsetPxX, canvasHeight / 2f + mapState.mapOffsetPxY)

            // 1. Draw Base Map Background according to MapType
            drawMapBaseLayer(
                mapType = mapState.mapType,
                center = center,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                lat = mapState.centerLat,
                lng = mapState.centerLng,
                zoom = mapState.zoomLevel,
                textMeasurer = textMeasurer
            )

            // 2. Draw LAZ Point Cloud Overlay Layer
            if (dataset != null && cachedPointCloudBitmap != null) {
                val scaleFactor = alignmentState.scaleX * (mapState.zoomLevel / 12f)
                val rotAngle = alignmentState.rotationDegrees

                // Calculate LAZ center screen position relative to map center
                val latDiff = (alignmentState.centerLat - mapState.centerLat) * 111139.0
                val lngDiff = (alignmentState.centerLng - mapState.centerLng) * (111139.0 * cos(Math.toRadians(mapState.centerLat)))

                val pixelsPerMeter = 1.2f * (mapState.zoomLevel / 15f)
                val lazCenterPx = Offset(
                    center.x + (lngDiff * pixelsPerMeter).toFloat(),
                    center.y - (latDiff * pixelsPerMeter).toFloat()
                )

                val datasetWidthPx = dataset.width * pixelsPerMeter * scaleFactor
                val datasetHeightPx = dataset.height * pixelsPerMeter * scaleFactor

                val left = lazCenterPx.x - datasetWidthPx / 2f
                val top = lazCenterPx.y - datasetHeightPx / 2f

                val curtainWidth = canvasWidth * alignmentState.splitCurtainRatio

                clipRect(right = curtainWidth) {
                    rotate(degrees = rotAngle, pivot = lazCenterPx) {
                        drawImage(
                            image = cachedPointCloudBitmap,
                            dstOffset = IntOffset(left.toInt(), top.toInt()),
                            dstSize = IntSize(
                                datasetWidthPx.toInt().coerceAtLeast(1),
                                datasetHeightPx.toInt().coerceAtLeast(1)
                            )
                        )

                        // Draw LAZ Bounding Box & Anchor Center Handle
                        drawRoundRect(
                            color = if (alignmentState.isLocked) ForestSageLight.copy(alpha = 0.5f) else HunterAmber,
                            topLeft = Offset(left, top),
                            size = Size(datasetWidthPx, datasetHeightPx),
                            cornerRadius = CornerRadius(4f, 4f),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                        )

                        // Center Crosshair for LAZ
                        drawCircle(
                            color = if (alignmentState.isLocked) ForestSagePrimary else HunterAmber,
                            radius = 8.dp.toPx(),
                            center = lazCenterPx,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = lazCenterPx
                        )

                        // Draw Anchor Label
                        val labelText = "${dataset.name} (${dataset.pointCount} pts)\nRot: ${rotAngle.roundToInt()}° | Scale: ${"%.2f".format(alignmentState.scaleX)}x"
                        drawText(
                            textMeasurer = textMeasurer,
                            text = labelText,
                            topLeft = Offset(left, top - 32.dp.toPx()),
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 11.sp,
                                background = Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                // Draw Split Curtain Line if curtain ratio < 1.0
                if (alignmentState.splitCurtainRatio < 1.0f) {
                    val curtainX = canvasWidth * alignmentState.splitCurtainRatio
                    drawLine(
                        color = HunterAmber,
                        start = Offset(curtainX, 0f),
                        end = Offset(curtainX, canvasHeight),
                        strokeWidth = 3.dp.toPx()
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "◀ LAZ LiDAR | Map Only ▶",
                        topLeft = Offset(curtainX - 80.dp.toPx(), 40.dp.toPx()),
                        style = TextStyle(color = HunterAmber, fontSize = 12.sp)
                    )
                }
            }

            // --- Machine Learning Overlays ---
            val pixelsPerMeter = 1.2f * (mapState.zoomLevel / 15f)

            // 2b. Draw Wildlife Corridors
            if (showCorridors && corridors.isNotEmpty()) {
                corridors.forEach { corridor ->
                    if (corridor.pathPoints.size > 1) {
                        val path = Path()
                        var validPoints = 0

                        corridor.pathPoints.forEach { pt ->
                            val px = center.x + (pt.first * pixelsPerMeter)
                            val py = center.y - (pt.second * pixelsPerMeter)
                            if (px.isFinite() && py.isFinite()) {
                                if (validPoints == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                validPoints++
                            }
                        }

                        if (validPoints > 1) {
                            drawPath(
                                path = path,
                                color = HunterAmber,
                                style = Stroke(
                                    width = 3.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                                )
                            )

                            // Corridor label
                            val startPt = corridor.pathPoints.first()
                            val lx = center.x + (startPt.first * pixelsPerMeter)
                            val ly = center.y - (startPt.second * pixelsPerMeter)
                            if (lx.isFinite() && ly.isFinite()) {
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = "🐾 ${corridor.name}",
                                    topLeft = Offset(lx, ly),
                                    style = TextStyle(color = HunterAmber, fontSize = 10.sp, background = Color.Black.copy(0.7f))
                                )
                            }
                        }
                    }
                }
            }

            // 2c. Draw Thermal Wind Vectors
            if (showThermals && thermalVectors.isNotEmpty()) {
                thermalVectors.forEach { vec ->
                    val vx = center.x + (vec.localX * pixelsPerMeter)
                    val vy = center.y - (vec.localY * pixelsPerMeter)
                    val arrowLen = (vec.speedMps * 8f).coerceIn(12f, 30f)
                    val rad = Math.toRadians(vec.windAngleDegrees.toDouble())

                    val endX = vx + (arrowLen * cos(rad)).toFloat()
                    val endY = vy + (arrowLen * sin(rad)).toFloat()

                    if (vx.isFinite() && vy.isFinite() && endX.isFinite() && endY.isFinite()) {
                        val color = if (vec.isDowndraft) TopoCyan.copy(alpha = 0.6f) else HunterAmber.copy(alpha = 0.6f)
                        drawLine(color = color, start = Offset(vx, vy), end = Offset(endX, endY), strokeWidth = 2f)
                        drawCircle(color = color, radius = 2f, center = Offset(endX, endY))
                    }
                }
            }

            // 2d. Draw Scent Plumes
            if (showThermals && scentPlumes.isNotEmpty()) {
                scentPlumes.forEach { plume ->
                    val px = center.x + (plume.originX * pixelsPerMeter)
                    val py = center.y - (plume.originY * pixelsPerMeter)
                    val reachPx = plume.reachMeters * pixelsPerMeter

                    if (px.isFinite() && py.isFinite() && reachPx.isFinite() && reachPx > 0f) {
                        drawArc(
                            color = Color.Red.copy(alpha = 0.22f),
                            startAngle = plume.windDirectionDegrees - (plume.coneAngleDegrees / 2f),
                            sweepAngle = plume.coneAngleDegrees,
                            useCenter = true,
                            topLeft = Offset(px - reachPx, py - reachPx),
                            size = Size(reachPx * 2, reachPx * 2)
                        )
                    }
                }
            }

            // 2e. Draw Topographic Feature Badges
            if (showFeatureHeatmap && !topographicGrid.isNullOrEmpty() && topographicGrid[0].isNotEmpty()) {
                val grid = topographicGrid!!
                for (y in grid.indices step 4) {
                    val row = grid[y]
                    for (x in row.indices step 4) {
                        val cell = row[x]
                        if (cell.featureType == TerrainFeatureType.SADDLE || cell.featureType == TerrainFeatureType.BENCH) {
                            val cx = center.x + (cell.worldX * pixelsPerMeter)
                            val cy = center.y - (cell.worldY * pixelsPerMeter)
                            if (!cx.isNaN() && !cy.isNaN() && cx.isFinite() && cy.isFinite()) {
                                drawCircle(
                                    color = Color(cell.featureType.colorHex).copy(alpha = 0.8f),
                                    radius = 6.dp.toPx(),
                                    center = Offset(cx, cy)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Draw Waypoints
            for (wp in waypoints) {
                val latDiff = (wp.lat - mapState.centerLat) * 111139.0
                val lngDiff = (wp.lng - mapState.centerLng) * (111139.0 * cos(Math.toRadians(mapState.centerLat)))
                val pixelsPerMeter = 1.2f * (mapState.zoomLevel / 15f)

                val wpX = (center.x + (lngDiff * pixelsPerMeter)).toFloat()
                val wpY = (center.y - (latDiff * pixelsPerMeter)).toFloat()

                // Pin circle
                drawCircle(
                    color = HunterAmber,
                    radius = 12.dp.toPx(),
                    center = Offset(wpX, wpY)
                )
                drawCircle(
                    color = Color.Black,
                    radius = 10.dp.toPx(),
                    center = Offset(wpX, wpY)
                )
                drawCircle(
                    color = LiDARGreen,
                    radius = 5.dp.toPx(),
                    center = Offset(wpX, wpY)
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = wp.title,
                    topLeft = Offset(wpX + 16.dp.toPx(), wpY - 10.dp.toPx()),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        background = Color.Black.copy(alpha = 0.75f)
                    )
                )
            }

            // 4. Center Map Target Crosshair if Crosshair Picker Active
            if (isCrosshairActive) {
                val chSize = 30.dp.toPx()
                drawLine(
                    color = LiDARGreen,
                    start = Offset(center.x - chSize, center.y),
                    end = Offset(center.x + chSize, center.y),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = LiDARGreen,
                    start = Offset(center.x, center.y - chSize),
                    end = Offset(center.x, center.y + chSize),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = LiDARGreen,
                    radius = 10.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Crosshair Confirm FAB
        if (isCrosshairActive) {
            Button(
                onClick = { onCrosshairPointSelected(mapState.centerLat, mapState.centerLng) },
                colors = ButtonDefaults.buttonColors(containerColor = LiDARGreen, contentColor = Color.Black),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 60.dp)
            ) {
                Icon(Icons.Default.PinDrop, contentDescription = "Set Anchor")
                Spacer(Modifier.width(8.dp))
                Text("Set LAZ Center Here", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private fun DrawScope.drawMapBaseLayer(
    mapType: MapType,
    center: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    lat: Double,
    lng: Double,
    zoom: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    // Background fill
    drawRect(color = Color(0xFF131A13))

    val tileZoom = zoom.toInt().coerceIn(1, 18)
    val tileSizePx = 256f

    val centerTileX = MapTileProvider.latLngToTileX(lat, lng, tileZoom)
    val centerTileY = MapTileProvider.latLngToTileY(lat, lng, tileZoom)

    val tilesXCount = (ceil(canvasWidth / tileSizePx).toInt() + 2).coerceIn(1, 12)
    val tilesYCount = (ceil(canvasHeight / tileSizePx).toInt() + 2).coerceIn(1, 12)

    val startTileX = floor(centerTileX - (canvasWidth / (2f * tileSizePx))).toInt() - 1
    val startTileY = floor(centerTileY - (canvasHeight / (2f * tileSizePx))).toInt() - 1

    var tilesDrawn = 0
    for (tx in startTileX until (startTileX + tilesXCount)) {
        for (ty in startTileY until (startTileY + tilesYCount)) {
            val tileBitmap = MapTileProvider.getTile(tileZoom, tx, ty, mapType)
            val tileScreenX = center.x + ((tx - centerTileX) * tileSizePx).toFloat()
            val tileScreenY = center.y + ((ty - centerTileY) * tileSizePx).toFloat()

            if (tileBitmap != null) {
                try {
                    drawImage(
                        image = tileBitmap,
                        topLeft = Offset(tileScreenX, tileScreenY)
                    )
                    tilesDrawn++
                } catch (e: Exception) {
                    // Ignore bitmap draw race condition
                }
            }
        }
    }

    if (tilesDrawn == 0) {
        // Fallback grid lines while tiles load
        val step = 80f
        var x = (center.x % step)
        while (x < canvasWidth) {
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(x, 0f),
                end = Offset(x, canvasHeight),
                strokeWidth = 1f
            )
            x += step
        }
        var y = (center.y % step)
        while (y < canvasHeight) {
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(canvasWidth, y),
                strokeWidth = 1f
            )
            y += step
        }
    }

    // Map Legend Info (Scale & GPS)
    val mapInfoText = "Center GPS: ${"%.4f".format(lat)}, ${"%.4f".format(lng)} | Zoom: ${"%.1f".format(zoom)} | Mode: ${mapType.label}"
    drawText(
        textMeasurer = textMeasurer,
        text = mapInfoText,
        topLeft = Offset(16.dp.toPx(), canvasHeight - 32.dp.toPx()),
        style = TextStyle(color = TextSecondaryLight, fontSize = 11.sp)
    )
}

/**
 * Calculates point color according to selected color ramp and elevation Z
 */
private fun getPointColor(
    pt: LazPoint,
    minZ: Float,
    maxZ: Float,
    colorRamp: ColorRamp,
    opacity: Float
): Color {
    val normZ = if (maxZ > minZ) ((pt.z - minZ) / (maxZ - minZ)).coerceIn(0f, 1f) else 0.5f

    val baseColor = when (colorRamp) {
        ColorRamp.TOPO_RAINBOW -> {
            when {
                normZ < 0.2f -> Color(0xFF0000FF) // Blue (Low / Valley)
                normZ < 0.4f -> Color(0xFF00FFFF) // Cyan
                normZ < 0.6f -> Color(0xFF00FF00) // Green
                normZ < 0.8f -> Color(0xFFFFFF00) // Yellow
                else -> Color(0xFFFF0000)         // Red (Peak)
            }
        }
        ColorRamp.ELEVATION_HEATMAP -> {
            Color(
                red = normZ,
                green = (1f - normZ) * 0.8f,
                blue = 0.1f,
                alpha = 1.0f
            )
        }
        ColorRamp.SLOPE_SHADING -> {
            val intensity = pt.intensity.coerceIn(0.2f, 1.0f)
            Color(
                red = 0.8f * intensity,
                green = 0.6f * intensity,
                blue = 0.3f * intensity,
                alpha = 1.0f
            )
        }
        ColorRamp.FOREST_CANOPY -> {
            if (pt.classification == 5) { // High veg
                Color(0xFF39FF14) // Neon Tree Canopy
            } else {
                Color(0xFF8B5A2B) // Ground Earth
            }
        }
        ColorRamp.GRAYSCALE_RELIEF -> {
            Color(normZ, normZ, normZ, 1.0f)
        }
    }

    return baseColor.copy(alpha = opacity)
}
