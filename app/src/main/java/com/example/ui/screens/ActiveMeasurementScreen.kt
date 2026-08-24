package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MapCanvas
import com.example.ui.theme.*
import com.example.ui.viewmodel.MeasurementUiState
import com.example.util.GeometryUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveMeasurementScreen(
    state: MeasurementUiState,
    onMarkPoint: () -> Unit,
    onUndoPoint: () -> Unit,
    onToggleClosePolygon: () -> Unit,
    onFinishMeasurement: () -> Unit,
    onBackToHome: () -> Unit,
    onSimulateMove: (dx: Double, dy: Double) -> Unit,
    onSetSimStep: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val nextLetter = GeometryUtils.indexToLetter(state.markedPoints.size)
    var showSimulationPanel by remember { mutableStateOf(state.isSimulationMode) }
    var showExitDialog by remember { mutableStateOf(false) }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Salir de la medición?") },
            text = { Text("Si sales sin finalizar, se perderán los puntos no guardados de esta sesión.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onBackToHome()
                }) {
                    Text("Salir", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continuar medición", color = PolishPrimary)
                }
            },
            containerColor = Color.White,
            titleContentColor = PolishTextPrimary,
            textContentColor = PolishTextSecondary
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Straighten,
                            contentDescription = null,
                            tint = PolishPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = state.sessionTitle.uppercase(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.markedPoints.isNotEmpty()) {
                            showExitDialog = true
                        } else {
                            onBackToHome()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = PolishTextPrimary)
                    }
                },
                actions = {
                    // GPS / Simulation status badge
                    Surface(
                        color = if (state.isSimulationMode) AccentYellowContainer else AccentGreenContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (state.isSimulationMode) AccentYellow else AccentGreen
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (state.isSimulationMode) AccentYellow else AccentGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.isSimulationMode) "SIMULACIÓN" else "GPS ±${String.format(Locale.US, "%.1f", state.accuracyMeters ?: 1.0)}m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isSimulationMode) AccentYellow else AccentGreen
                            )
                        }
                    }

                    // Finish check button in top bar
                    IconButton(
                        onClick = onFinishMeasurement,
                        enabled = state.markedPoints.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Finalizar medición",
                            tint = if (state.markedPoints.isNotEmpty()) PolishPrimary else TextTertiary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PolishBg
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(PolishBg)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Secondary Action Row (Close polygon, Undo, Terminate)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Toggle Close Polygon Button
                    OutlinedButton(
                        onClick = onToggleClosePolygon,
                        enabled = state.markedPoints.size >= 3,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (state.isPolygonClosed) PolishPrimaryContainer else Color.White,
                            contentColor = if (state.isPolygonClosed) PolishPrimaryDark else PolishTextPrimary
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (state.isPolygonClosed) PolishPrimary else PolishCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.1f).height(44.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isPolygonClosed) Icons.Default.Polyline else Icons.Default.LinearScale,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (state.isPolygonClosed) "Polígono OK" else "Cerrar Área",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Undo Point Button
                    OutlinedButton(
                        onClick = onUndoPoint,
                        enabled = state.markedPoints.isNotEmpty(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = PolishTextPrimary
                        ),
                        border = BorderStroke(1.dp, PolishCardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.85f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Deshacer", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Deshacer", fontSize = 12.sp)
                    }

                    // Finish measurement button
                    Button(
                        onClick = onFinishMeasurement,
                        enabled = state.markedPoints.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PolishPrimaryContainer,
                            contentColor = PolishPrimaryDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.95f).height(44.dp)
                    ) {
                        Text(text = "Terminar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Primary Large Action Button: MARK POINT (A, B, C...)
                Button(
                    onClick = onMarkPoint,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PolishPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(18.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("mark_point_button")
                ) {
                    Icon(
                        Icons.Default.AddLocationAlt,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MARCAR PUNTO ($nextLetter)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        },
        containerColor = PolishBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Distance & Area Display Card
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, PolishCardBorder),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total Distance
                    Column {
                        Text(
                            text = "DISTANCIA TOTAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.US, "%.2f", state.totalDistanceMeters),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishPrimary
                            )
                            Text(
                                text = " m",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${state.totalDistanceCm} cm)",
                                fontSize = 12.sp,
                                color = PolishTextTertiary,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    // Total Area or Puntos
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (state.isPolygonClosed && state.markedPoints.size >= 3) "ÁREA DEL TERRENO" else "PUNTOS MARCADOS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        if (state.isPolygonClosed && state.markedPoints.size >= 3) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format(Locale.US, "%.2f", state.areaSquareMeters),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishTextPrimary
                                )
                                Text(
                                    text = " m²",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishPrimary
                                )
                            }
                        } else {
                            Text(
                                text = "${state.markedPoints.size} Puntos",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                        }
                    }
                }
            }

            // Interactive Map Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                MapCanvas(
                    markedPoints = state.markedPoints,
                    segments = state.segments,
                    gpsBreadcrumbs = state.gpsBreadcrumbs,
                    currentLocation = state.currentLocation,
                    bearing = state.bearing,
                    isPolygonClosed = state.isPolygonClosed,
                    isInteractive = true,
                    showDistanceLabels = true,
                    showBreadcrumbs = true,
                    modifier = Modifier.fillMaxSize()
                )

                // Floating Simulation D-Pad toggle button
                IconButton(
                    onClick = { showSimulationPanel = !showSimulationPanel },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (showSimulationPanel) PolishPrimary else Color.White)
                        .border(BorderStroke(1.dp, PolishCardBorder), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Gamepad,
                        contentDescription = "Control de caminata virtual",
                        tint = if (showSimulationPanel) Color.White else PolishPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Virtual Walk / Simulation Controls Drawer
            AnimatedVisibility(visible = showSimulationPanel) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, PolishCardBorder),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Control de Caminata Virtual (Simulación)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishPrimary
                            )

                            // Step selector: 1m, 5m, 10m
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(1.0, 5.0, 10.0).forEach { step ->
                                    FilterChip(
                                        selected = state.simulationStepMeters == step,
                                        onClick = { onSetSimStep(step) },
                                        label = { Text("${step.toInt()}m", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PolishPrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // D-Pad Walk buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // West (Left)
                            IconButton(
                                onClick = { onSimulateMove(-state.simulationStepMeters, 0.0) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(PolishSurfaceVariant)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Oeste / Izquierda", tint = PolishTextPrimary)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // North & South
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { onSimulateMove(0.0, state.simulationStepMeters) },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(PolishSurfaceVariant)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Norte / Adelante", tint = PolishTextPrimary)
                                }
                                IconButton(
                                    onClick = { onSimulateMove(0.0, -state.simulationStepMeters) },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(PolishSurfaceVariant)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Sur / Atrás", tint = PolishTextPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // East (Right)
                            IconButton(
                                onClick = { onSimulateMove(state.simulationStepMeters, 0.0) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(PolishSurfaceVariant)
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Este / Derecha", tint = PolishTextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

