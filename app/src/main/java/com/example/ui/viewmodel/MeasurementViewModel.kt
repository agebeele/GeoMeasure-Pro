package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.GeoPoint
import com.example.data.model.LocationPoint
import com.example.data.model.MeasurementRecord
import com.example.data.model.SegmentMeasure
import com.example.data.repository.MeasurementRepository
import com.example.location.LocationTracker
import com.example.util.GeometryUtils
import com.example.util.ImageExportUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
    HOME,
    ACTIVE_MEASUREMENT,
    COMPLETED_SUMMARY,
    HISTORY,
    DETAIL_VIEW
}

data class MeasurementUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val isTracking: Boolean = false,
    val isSimulationMode: Boolean = false,
    val currentLocation: Location? = null,
    val accuracyMeters: Float? = null,
    val bearing: Float? = null,
    val gpsBreadcrumbs: List<LocationPoint> = emptyList(),
    val markedPoints: List<GeoPoint> = emptyList(),
    val segments: List<SegmentMeasure> = emptyList(),
    val totalDistanceMeters: Double = 0.0,
    val totalDistanceCm: Long = 0L,
    val areaSquareMeters: Double = 0.0,
    val isPolygonClosed: Boolean = false,
    val sessionTitle: String = "Medición 001",
    val sessionStartTime: Long = System.currentTimeMillis(),
    val simulationStepMeters: Double = 5.0,
    val selectedRecord: MeasurementRecord? = null,
    val isSaved: Boolean = false,
    val snackbarMessage: String? = null
)

class MeasurementViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MeasurementRepository
    val locationTracker: LocationTracker = LocationTracker(application)

    private val _uiState = MutableStateFlow(MeasurementUiState())
    val uiState: StateFlow<MeasurementUiState> = _uiState.asStateFlow()

    val historyRecords: StateFlow<List<MeasurementRecord>>

    private var sessionCounter = 1

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MeasurementRepository(database.measurementDao())
        historyRecords = repository.allMeasurements.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Observe location tracker
        viewModelScope.launch {
            locationTracker.currentLocation.collect { loc ->
                if (loc != null) {
                    _uiState.update { state ->
                        val updatedLocList = if (state.isTracking) {
                            val newPt = LocationPoint(loc.latitude, loc.longitude, loc.altitude)
                            // Filter tiny jitter (less than 0.3m from last breadcrumb)
                            val last = state.gpsBreadcrumbs.lastOrNull()
                            if (last == null || GeometryUtils.calculateDistance(last.latitude, last.longitude, loc.latitude, loc.longitude) > 0.3) {
                                state.gpsBreadcrumbs + newPt
                            } else {
                                state.gpsBreadcrumbs
                            }
                        } else {
                            state.gpsBreadcrumbs
                        }
                        state.copy(
                            currentLocation = loc,
                            accuracyMeters = loc.accuracy,
                            bearing = loc.bearing,
                            gpsBreadcrumbs = updatedLocList
                        )
                    }
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun startNewSession(isSimulation: Boolean = false) {
        val nextLetter = GeometryUtils.indexToLetter(0) // "A"
        val title = "Medición #${String.format(Locale.US, "%03d", sessionCounter++)}"
        _uiState.update {
            MeasurementUiState(
                currentScreen = AppScreen.ACTIVE_MEASUREMENT,
                isTracking = true,
                isSimulationMode = isSimulation,
                sessionTitle = title,
                sessionStartTime = System.currentTimeMillis(),
                gpsBreadcrumbs = emptyList(),
                markedPoints = emptyList(),
                segments = emptyList(),
                totalDistanceMeters = 0.0,
                totalDistanceCm = 0L,
                areaSquareMeters = 0.0,
                isPolygonClosed = false,
                isSaved = false
            )
        }
        locationTracker.startTracking(isSimulation)
    }

    fun markCurrentPoint() {
        val loc = _uiState.value.currentLocation ?: run {
            _uiState.update { it.copy(snackbarMessage = "Esperando señal GPS...") }
            return
        }

        val currentPoints = _uiState.value.markedPoints
        val nextIndex = currentPoints.size
        val letter = GeometryUtils.indexToLetter(nextIndex)

        var distFromPrev = 0.0
        if (currentPoints.isNotEmpty()) {
            val lastPoint = currentPoints.last()
            distFromPrev = GeometryUtils.calculateDistance(
                lastPoint.latitude,
                lastPoint.longitude,
                loc.latitude,
                loc.longitude
            )
        }

        val newGeoPoint = GeoPoint(
            letter = letter,
            latitude = loc.latitude,
            longitude = loc.longitude,
            distanceFromPrevMeters = distFromPrev,
            distanceFromPrevCm = GeometryUtils.metersToCm(distFromPrev),
            timestamp = System.currentTimeMillis()
        )

        val updatedPoints = currentPoints + newGeoPoint
        recalculateMetrics(updatedPoints, _uiState.value.isPolygonClosed)

        val msg = if (currentPoints.isEmpty()) {
            "Punto A fijado en inicio"
        } else {
            "Punto $letter marcado: ${String.format(Locale.US, "%.2fm", distFromPrev)} (${GeometryUtils.metersToCm(distFromPrev)}cm) desde ${currentPoints.last().letter}"
        }
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun addManualPoint(lat: Double, lon: Double) {
        val currentPoints = _uiState.value.markedPoints
        val nextIndex = currentPoints.size
        val letter = GeometryUtils.indexToLetter(nextIndex)

        var distFromPrev = 0.0
        if (currentPoints.isNotEmpty()) {
            val lastPoint = currentPoints.last()
            distFromPrev = GeometryUtils.calculateDistance(
                lastPoint.latitude,
                lastPoint.longitude,
                lat,
                lon
            )
        }

        val newGeoPoint = GeoPoint(
            letter = letter,
            latitude = lat,
            longitude = lon,
            distanceFromPrevMeters = distFromPrev,
            distanceFromPrevCm = GeometryUtils.metersToCm(distFromPrev),
            timestamp = System.currentTimeMillis()
        )

        val updatedPoints = currentPoints + newGeoPoint
        recalculateMetrics(updatedPoints, _uiState.value.isPolygonClosed)
        _uiState.update { it.copy(snackbarMessage = "Punto $letter añadido manualmente") }
    }

    fun undoLastPoint() {
        val currentPoints = _uiState.value.markedPoints
        if (currentPoints.isEmpty()) return
        val removed = currentPoints.last()
        val updatedPoints = currentPoints.dropLast(1)
        recalculateMetrics(updatedPoints, false)
        _uiState.update {
            it.copy(
                isPolygonClosed = false,
                snackbarMessage = "Punto ${removed.letter} eliminado"
            )
        }
    }

    fun toggleClosePolygon() {
        val currentPoints = _uiState.value.markedPoints
        if (currentPoints.size < 3) {
            _uiState.update { it.copy(snackbarMessage = "Se necesitan al menos 3 puntos (A, B, C) para cerrar el área") }
            return
        }
        val willClose = !_uiState.value.isPolygonClosed
        recalculateMetrics(currentPoints, willClose)
        _uiState.update {
            it.copy(
                isPolygonClosed = willClose,
                snackbarMessage = if (willClose) "Polígono cerrado. Área calculada." else "Polígono abierto."
            )
        }
    }

    private fun recalculateMetrics(points: List<GeoPoint>, isClosed: Boolean) {
        val segments = GeometryUtils.buildSegments(points, isClosed)
        val totalDistance = segments.sumOf { it.distanceMeters }
        val totalCm = GeometryUtils.metersToCm(totalDistance)
        val area = if (isClosed && points.size >= 3) {
            GeometryUtils.calculatePolygonArea(points)
        } else {
            0.0
        }

        _uiState.update {
            it.copy(
                markedPoints = points,
                segments = segments,
                totalDistanceMeters = totalDistance,
                totalDistanceCm = totalCm,
                areaSquareMeters = area,
                isPolygonClosed = isClosed
            )
        }
    }

    fun completeMeasurement() {
        locationTracker.stopTracking()
        _uiState.update {
            it.copy(
                isTracking = false,
                currentScreen = AppScreen.COMPLETED_SUMMARY
            )
        }
    }

    fun saveCurrentMeasurement() {
        val state = _uiState.value
        if (state.markedPoints.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "No hay puntos marcados para guardar") }
            return
        }

        val record = MeasurementRecord(
            title = state.sessionTitle,
            timestamp = state.sessionStartTime,
            totalDistanceMeters = state.totalDistanceMeters,
            totalDistanceCm = state.totalDistanceCm,
            areaSquareMeters = state.areaSquareMeters,
            isClosedPolygon = state.isPolygonClosed,
            pointsCount = state.markedPoints.size,
            pointsJson = GeometryUtils.pointsToJson(state.markedPoints),
            segmentsJson = GeometryUtils.segmentsToJson(state.segments)
        )

        viewModelScope.launch {
            repository.insertMeasurement(record)
            _uiState.update {
                it.copy(
                    isSaved = true,
                    snackbarMessage = "Medición guardada en el historial con éxito"
                )
            }
        }
    }

    fun selectRecordForDetail(record: MeasurementRecord) {
        _uiState.update {
            it.copy(
                selectedRecord = record,
                currentScreen = AppScreen.DETAIL_VIEW
            )
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteMeasurement(id)
            if (_uiState.value.selectedRecord?.id == id) {
                _uiState.update {
                    it.copy(
                        selectedRecord = null,
                        currentScreen = AppScreen.HISTORY,
                        snackbarMessage = "Registro eliminado"
                    )
                }
            } else {
                _uiState.update { it.copy(snackbarMessage = "Registro eliminado") }
            }
        }
    }

    fun setSimulationStepMeters(step: Double) {
        _uiState.update { it.copy(simulationStepMeters = step) }
    }

    fun simulateMoveDirection(dxMeters: Double, dyMeters: Double) {
        locationTracker.simulateMove(dxMeters, dyMeters)
    }

    fun shareCurrentMapImage(context: Context) {
        val state = _uiState.value
        val points = if (state.currentScreen == AppScreen.DETAIL_VIEW && state.selectedRecord != null) {
            GeometryUtils.jsonToPoints(state.selectedRecord.pointsJson)
        } else {
            state.markedPoints
        }

        val segments = if (state.currentScreen == AppScreen.DETAIL_VIEW && state.selectedRecord != null) {
            GeometryUtils.jsonToSegments(state.selectedRecord.segmentsJson)
        } else {
            state.segments
        }

        val title = if (state.currentScreen == AppScreen.DETAIL_VIEW && state.selectedRecord != null) {
            state.selectedRecord.title
        } else {
            state.sessionTitle
        }

        val totalDist = if (state.currentScreen == AppScreen.DETAIL_VIEW && state.selectedRecord != null) {
            state.selectedRecord.totalDistanceMeters
        } else {
            state.totalDistanceMeters
        }

        val area = if (state.currentScreen == AppScreen.DETAIL_VIEW && state.selectedRecord != null) {
            state.selectedRecord.areaSquareMeters
        } else {
            state.areaSquareMeters
        }

        val isClosed = if (state.currentScreen == AppScreen.DETAIL_VIEW && state.selectedRecord != null) {
            state.selectedRecord.isClosedPolygon
        } else {
            state.isPolygonClosed
        }

        val timestamp = if (state.currentScreen == AppScreen.DETAIL_VIEW && state.selectedRecord != null) {
            state.selectedRecord.timestamp
        } else {
            state.sessionStartTime
        }

        ImageExportUtils.shareMeasurementImage(
            context = context,
            title = title,
            timestamp = timestamp,
            totalDistanceMeters = totalDist,
            areaSquareMeters = area,
            isClosed = isClosed,
            points = points,
            segments = segments
        )
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
