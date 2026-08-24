package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MeasurementViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MeasurementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val historyList by viewModel.historyRecords.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                // Check permissions
                var hasFineLocation by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    hasFineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (hasFineLocation) {
                        viewModel.startNewSession(isSimulation = false)
                    } else {
                        // Start simulation mode if permission denied
                        viewModel.startNewSession(isSimulation = true)
                    }
                }

                // Handle Snackbar messages from ViewModel
                LaunchedEffect(uiState.snackbarMessage) {
                    uiState.snackbarMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                        viewModel.clearSnackbar()
                    }
                }

                Scaffold(
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    },
                    containerColor = DarkBackground,
                    modifier = Modifier.fillMaxSize()
                ) { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        when (uiState.currentScreen) {
                            AppScreen.HOME -> {
                                HomeScreen(
                                    onStartNewMeasurement = {
                                        if (hasFineLocation) {
                                            viewModel.startNewSession(isSimulation = false)
                                        } else {
                                            permissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    },
                                    onStartSimulationMode = {
                                        viewModel.startNewSession(isSimulation = true)
                                    },
                                    onViewHistory = {
                                        viewModel.navigateTo(AppScreen.HISTORY)
                                    },
                                    historyCount = historyList.size
                                )
                            }

                            AppScreen.ACTIVE_MEASUREMENT -> {
                                ActiveMeasurementScreen(
                                    state = uiState,
                                    onMarkPoint = { viewModel.markCurrentPoint() },
                                    onUndoPoint = { viewModel.undoLastPoint() },
                                    onToggleClosePolygon = { viewModel.toggleClosePolygon() },
                                    onFinishMeasurement = { viewModel.completeMeasurement() },
                                    onBackToHome = {
                                        viewModel.locationTracker.stopTracking()
                                        viewModel.navigateTo(AppScreen.HOME)
                                    },
                                    onSimulateMove = { dx, dy -> viewModel.simulateMoveDirection(dx, dy) },
                                    onSetSimStep = { step -> viewModel.setSimulationStepMeters(step) }
                                )
                            }

                            AppScreen.COMPLETED_SUMMARY -> {
                                CompletedMeasurementScreen(
                                    state = uiState,
                                    onSaveToHistory = { viewModel.saveCurrentMeasurement() },
                                    onShareImage = { viewModel.shareCurrentMapImage(context) },
                                    onStartNew = {
                                        viewModel.startNewSession(isSimulation = uiState.isSimulationMode)
                                    },
                                    onViewHistory = {
                                        viewModel.navigateTo(AppScreen.HISTORY)
                                    },
                                    onBack = {
                                        viewModel.navigateTo(AppScreen.HOME)
                                    }
                                )
                            }

                            AppScreen.HISTORY -> {
                                HistoryScreen(
                                    records = historyList,
                                    onSelectRecord = { record ->
                                        viewModel.selectRecordForDetail(record)
                                    },
                                    onDeleteRecord = { id ->
                                        viewModel.deleteRecord(id)
                                    },
                                    onBack = {
                                        viewModel.navigateTo(AppScreen.HOME)
                                    },
                                    onStartNew = {
                                        viewModel.startNewSession(isSimulation = false)
                                    }
                                )
                            }

                            AppScreen.DETAIL_VIEW -> {
                                uiState.selectedRecord?.let { record ->
                                    DetailMeasurementScreen(
                                        record = record,
                                        onShareImage = { viewModel.shareCurrentMapImage(context) },
                                        onDelete = { id -> viewModel.deleteRecord(id) },
                                        onBack = { viewModel.navigateTo(AppScreen.HISTORY) }
                                    )
                                } ?: run {
                                    viewModel.navigateTo(AppScreen.HISTORY)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
