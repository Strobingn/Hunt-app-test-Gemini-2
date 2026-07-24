package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.ColorRamp
import com.example.data.model.HuntingWaypoint
import com.example.data.model.LazDataset
import com.example.data.model.SavedOverlay
import com.example.data.model.WaypointType
import com.example.data.repository.HuntMapRepository
import com.example.data.service.AiTerrainService
import com.example.ml.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MapType(val label: String) {
    SATELLITE("Satellite"),
    HYBRID("Hybrid"),
    TERRAIN("Terrain Topo"),
    DARK_VECTOR("Dark Vector")
}

data class MapState(
    val mapType: MapType = MapType.SATELLITE,
    val centerLat: Double = 44.9812,
    val centerLng: Double = -110.6923,
    val zoomLevel: Float = 15.5f,
    val mapOffsetPxX: Float = 0f,
    val mapOffsetPxY: Float = 0f
)

data class AlignmentState(
    val lazDataset: LazDataset? = null,
    val activeOverlayId: Long = 0L,
    val centerLat: Double = 44.9812,
    val centerLng: Double = -110.6923,
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val rotationDegrees: Float = 0.0f,
    val opacity: Float = 0.8f,
    val colorRamp: ColorRamp = ColorRamp.TOPO_RAINBOW,
    val minElevationFilter: Float = 0.0f, // 0.0 to 1.0 fraction of minZ..maxZ
    val maxElevationFilter: Float = 1.0f,
    val isLocked: Boolean = false,
    val nudgeStepMeters: Float = 1.0f, // 0.1, 1.0, 10.0, 50.0
    val splitCurtainRatio: Float = 1.0f // 1.0 = full overlay, 0.5 = 50% split view curtain
)

class HuntMapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HuntMapRepository

    val savedOverlays: StateFlow<List<SavedOverlay>>
    val waypoints: StateFlow<List<HuntingWaypoint>>

    val sampleDatasets: List<LazDataset>

    private val _mapState = MutableStateFlow(MapState())
    val mapState: StateFlow<MapState> = _mapState.asStateFlow()

    private val _alignmentState = MutableStateFlow(AlignmentState())
    val alignmentState: StateFlow<AlignmentState> = _alignmentState.asStateFlow()

    private val _selectedWaypointForDetails = MutableStateFlow<HuntingWaypoint?>(null)
    val selectedWaypointForDetails: StateFlow<HuntingWaypoint?> = _selectedWaypointForDetails.asStateFlow()

    private val _isCrosshairPickerActive = MutableStateFlow(false)
    val isCrosshairPickerActive: StateFlow<Boolean> = _isCrosshairPickerActive.asStateFlow()

    private val _oracleBackendUrl = MutableStateFlow("")
    val oracleBackendUrl: StateFlow<String> = _oracleBackendUrl.asStateFlow()

    private val _isAnalyzingAi = MutableStateFlow(false)
    val isAnalyzingAi: StateFlow<Boolean> = _isAnalyzingAi.asStateFlow()

    private val _aiAnalysisResult = MutableStateFlow<String?>(null)
    val aiAnalysisResult: StateFlow<String?> = _aiAnalysisResult.asStateFlow()

    // Machine Learning Heavy Overlay States
    private val _showCorridors = MutableStateFlow(true)
    val showCorridors: StateFlow<Boolean> = _showCorridors.asStateFlow()

    private val _showThermals = MutableStateFlow(true)
    val showThermals: StateFlow<Boolean> = _showThermals.asStateFlow()

    private val _showFeatureHeatmap = MutableStateFlow(false)
    val showFeatureHeatmap: StateFlow<Boolean> = _showFeatureHeatmap.asStateFlow()

    private val _timeOfDay = MutableStateFlow(ThermalScentSimulator.TimeOfDay.DAWN_MORNING)
    val timeOfDay: StateFlow<ThermalScentSimulator.TimeOfDay> = _timeOfDay.asStateFlow()

    private val _topographicGrid = MutableStateFlow<Array<Array<TerrainCell>>?>(null)
    val topographicGrid: StateFlow<Array<Array<TerrainCell>>?> = _topographicGrid.asStateFlow()

    private val _corridors = MutableStateFlow<List<WildlifeCorridor>>(emptyList())
    val corridors: StateFlow<List<WildlifeCorridor>> = _corridors.asStateFlow()

    private val _thermalVectors = MutableStateFlow<List<ThermalWindVector>>(emptyList())
    val thermalVectors: StateFlow<List<ThermalWindVector>> = _thermalVectors.asStateFlow()

    private val _scentPlumes = MutableStateFlow<List<ScentPlumeCone>>(emptyList())
    val scentPlumes: StateFlow<List<ScentPlumeCone>> = _scentPlumes.asStateFlow()

    private val aiTerrainService = AiTerrainService()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = HuntMapRepository(db.savedOverlayDao(), db.huntingWaypointDao())

        savedOverlays = repository.allSavedOverlays.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        waypoints = repository.allWaypoints.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        sampleDatasets = repository.getSampleDatasets()
        if (sampleDatasets.isNotEmpty()) {
            selectDataset(sampleDatasets.first())
        }
    }

    fun selectDataset(dataset: LazDataset) {
        _alignmentState.value = _alignmentState.value.copy(
            lazDataset = dataset,
            activeOverlayId = 0L,
            centerLat = dataset.defaultLat,
            centerLng = dataset.defaultLng,
            scaleX = 1.0f,
            scaleY = 1.0f,
            rotationDegrees = 0.0f,
            opacity = 0.8f,
            isLocked = false
        )
        _mapState.value = _mapState.value.copy(
            centerLat = dataset.defaultLat,
            centerLng = dataset.defaultLng
        )
        recalculateMlOverlays(dataset)
    }

    fun toggleCorridors() {
        _showCorridors.value = !_showCorridors.value
    }

    fun toggleThermals() {
        _showThermals.value = !_showThermals.value
    }

    fun toggleFeatureHeatmap() {
        _showFeatureHeatmap.value = !_showFeatureHeatmap.value
    }

    fun setTimeOfDay(time: ThermalScentSimulator.TimeOfDay) {
        _timeOfDay.value = time
        _alignmentState.value.lazDataset?.let { recalculateMlOverlays(it) }
    }

    fun recalculateMlOverlays(dataset: LazDataset) {
        viewModelScope.launch(Dispatchers.Default) {
            val grid = TerrainFeatureExtractor.processDataset(dataset)
            _topographicGrid.value = grid

            val generatedCorridors = CorridorPathfinder.generateWildlifeCorridors(grid)
            _corridors.value = generatedCorridors

            val vectors = ThermalScentSimulator.calculateWindVectors(grid, _timeOfDay.value)
            _thermalVectors.value = vectors

            val plumes = waypoints.value.map { wp ->
                ThermalScentSimulator.generateScentPlume(
                    standX = ((wp.lng - dataset.defaultLng) * 111320.0).toFloat(),
                    standY = ((wp.lat - dataset.defaultLat) * 111320.0).toFloat(),
                    windDirectionDegrees = vectors.firstOrNull()?.windAngleDegrees ?: 270f,
                    windSpeedMps = 2.5f
                )
            }
            _scentPlumes.value = plumes
        }
    }

    fun setMapType(type: MapType) {
        _mapState.value = _mapState.value.copy(mapType = type)
    }

    fun updateMapCamera(lat: Double, lng: Double, zoom: Float) {
        _mapState.value = _mapState.value.copy(
            centerLat = lat,
            centerLng = lng,
            zoomLevel = zoom
        )
    }

    fun panMapBy(dxPx: Float, dyPx: Float) {
        _mapState.value = _mapState.value.copy(
            mapOffsetPxX = _mapState.value.mapOffsetPxX + dxPx,
            mapOffsetPxY = _mapState.value.mapOffsetPxY + dyPx
        )
    }

    fun setCenterLat(lat: Double) {
        if (_alignmentState.value.isLocked) return
        _alignmentState.value = _alignmentState.value.copy(centerLat = lat)
    }

    fun setCenterLng(lng: Double) {
        if (_alignmentState.value.isLocked) return
        _alignmentState.value = _alignmentState.value.copy(centerLng = lng)
    }

    fun snapAlignmentToMapCenter() {
        if (_alignmentState.value.isLocked) return
        _alignmentState.value = _alignmentState.value.copy(
            centerLat = _mapState.value.centerLat,
            centerLng = _mapState.value.centerLng
        )
    }

    /**
     * Nudge LAZ overlay position by North, South, East, West in meters
     */
    fun nudgeOverlay(deltaNorthMeters: Float, deltaEastMeters: Float) {
        if (_alignmentState.value.isLocked) return
        val step = _alignmentState.value.nudgeStepMeters
        val latDegPerMeter = 1.0 / 111139.0
        val lngDegPerMeter = 1.0 / (111139.0 * Math.cos(Math.toRadians(_alignmentState.value.centerLat)))

        val dLat = deltaNorthMeters * step * latDegPerMeter
        val dLng = deltaEastMeters * step * lngDegPerMeter

        _alignmentState.value = _alignmentState.value.copy(
            centerLat = _alignmentState.value.centerLat + dLat,
            centerLng = _alignmentState.value.centerLng + dLng
        )
    }

    fun setNudgeStep(stepMeters: Float) {
        _alignmentState.value = _alignmentState.value.copy(nudgeStepMeters = stepMeters)
    }

    fun updateScale(scaleX: Float, scaleY: Float = scaleX) {
        if (_alignmentState.value.isLocked) return
        _alignmentState.value = _alignmentState.value.copy(
            scaleX = scaleX.coerceIn(0.1f, 10.0f),
            scaleY = scaleY.coerceIn(0.1f, 10.0f)
        )
    }

    fun updateRotation(degrees: Float) {
        if (_alignmentState.value.isLocked) return
        val normalized = (degrees % 360f + 360f) % 360f
        _alignmentState.value = _alignmentState.value.copy(rotationDegrees = normalized)
    }

    fun updateOpacity(opacity: Float) {
        _alignmentState.value = _alignmentState.value.copy(opacity = opacity.coerceIn(0.0f, 1.0f))
    }

    fun setColorRamp(colorRamp: ColorRamp) {
        _alignmentState.value = _alignmentState.value.copy(colorRamp = colorRamp)
    }

    fun setElevationFilter(minFrac: Float, maxFrac: Float) {
        _alignmentState.value = _alignmentState.value.copy(
            minElevationFilter = minFrac.coerceIn(0.0f, maxFrac),
            maxElevationFilter = maxFrac.coerceIn(minFrac, 1.0f)
        )
    }

    fun setSplitCurtainRatio(ratio: Float) {
        _alignmentState.value = _alignmentState.value.copy(splitCurtainRatio = ratio.coerceIn(0.0f, 1.0f))
    }

    fun toggleLock() {
        _alignmentState.value = _alignmentState.value.copy(isLocked = !_alignmentState.value.isLocked)
    }

    fun saveCurrentOverlayPreset(name: String) {
        val curr = _alignmentState.value
        val dataset = curr.lazDataset ?: return

        viewModelScope.launch {
            val saved = SavedOverlay(
                id = curr.activeOverlayId,
                name = name.ifBlank { "${dataset.name} Alignment" },
                lazDatasetId = dataset.id,
                centerLat = curr.centerLat,
                centerLng = curr.centerLng,
                scaleX = curr.scaleX,
                scaleY = curr.scaleY,
                rotationDegrees = curr.rotationDegrees,
                opacity = curr.opacity,
                colorRampName = curr.colorRamp.name,
                minElevationFilter = curr.minElevationFilter,
                maxElevationFilter = curr.maxElevationFilter,
                isLocked = curr.isLocked
            )
            val id = repository.saveOverlay(saved)
            _alignmentState.value = _alignmentState.value.copy(activeOverlayId = id)
        }
    }

    fun loadSavedOverlay(overlay: SavedOverlay) {
        val matchingDataset = sampleDatasets.find { it.id == overlay.lazDatasetId }
            ?: sampleDatasets.firstOrNull() ?: return

        val colorRamp = try {
            ColorRamp.valueOf(overlay.colorRampName)
        } catch (e: Exception) {
            ColorRamp.TOPO_RAINBOW
        }

        _alignmentState.value = AlignmentState(
            lazDataset = matchingDataset,
            activeOverlayId = overlay.id,
            centerLat = overlay.centerLat,
            centerLng = overlay.centerLng,
            scaleX = overlay.scaleX,
            scaleY = overlay.scaleY,
            rotationDegrees = overlay.rotationDegrees,
            opacity = overlay.opacity,
            colorRamp = colorRamp,
            minElevationFilter = overlay.minElevationFilter,
            maxElevationFilter = overlay.maxElevationFilter,
            isLocked = overlay.isLocked
        )

        _mapState.value = _mapState.value.copy(
            centerLat = overlay.centerLat,
            centerLng = overlay.centerLng
        )
    }

    fun deleteSavedOverlay(overlay: SavedOverlay) {
        viewModelScope.launch {
            repository.deleteOverlay(overlay)
        }
    }

    fun addWaypoint(title: String, note: String, type: WaypointType, lat: Double, lng: Double, elevation: Float) {
        viewModelScope.launch {
            repository.addWaypoint(
                HuntingWaypoint(
                    overlayId = _alignmentState.value.activeOverlayId,
                    title = title,
                    note = note,
                    type = type,
                    lat = lat,
                    lng = lng,
                    elevationMeters = elevation
                )
            )
        }
    }

    fun deleteWaypoint(waypoint: HuntingWaypoint) {
        viewModelScope.launch {
            repository.deleteWaypoint(waypoint)
        }
    }

    fun selectWaypointDetails(waypoint: HuntingWaypoint?) {
        _selectedWaypointForDetails.value = waypoint
    }

    fun setCrosshairPickerActive(active: Boolean) {
        _isCrosshairPickerActive.value = active
    }

    fun setOracleBackendUrl(url: String) {
        _oracleBackendUrl.value = url.trim()
    }

    fun runAiTerrainAnalysis(customPrompt: String? = null) {
        viewModelScope.launch {
            _isAnalyzingAi.value = true
            try {
                val result = aiTerrainService.analyzeTerrain(
                    dataset = _alignmentState.value.lazDataset,
                    centerLat = _alignmentState.value.centerLat,
                    centerLng = _alignmentState.value.centerLng,
                    waypoints = waypoints.value,
                    customUserPrompt = customPrompt,
                    oracleBackendUrl = _oracleBackendUrl.value
                )
                _aiAnalysisResult.value = result
            } catch (e: Exception) {
                _aiAnalysisResult.value = "Error performing AI terrain analysis: ${e.message}"
            } finally {
                _isAnalyzingAi.value = false
            }
        }
    }
}
