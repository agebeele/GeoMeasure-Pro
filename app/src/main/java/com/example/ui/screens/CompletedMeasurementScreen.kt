package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MapCanvas
import com.example.ui.theme.*
import com.example.ui.viewmodel.MeasurementUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedMeasurementScreen(
    state: MeasurementUiState,
    onSaveToHistory: () -> Unit,
    onShareImage: () -> Unit,
    onStartNew: () -> Unit,
    onViewHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                            text = "MEDICIÓN COMPLETADA",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = PolishTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onViewHistory) {
                        Icon(Icons.Default.History, contentDescription = "Ver historial", tint = PolishPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PolishBg)
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
                // Save to history button
                Button(
                    onClick = onSaveToHistory,
                    enabled = !state.isSaved,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isSaved) PolishSurfaceVariant else PolishPrimary,
                        contentColor = if (state.isSaved) PolishTextSecondary else Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_recorrido_button")
                ) {
                    Icon(
                        imageVector = if (state.isSaved) Icons.Default.CheckCircle else Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isSaved) "GUARDADO EN EL HISTORIAL" else "GUARDAR RECORRIDO EN HISTORIAL",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Share map image button
                OutlinedButton(
                    onClick = onShareImage,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = PolishPrimary
                    ),
                    border = BorderStroke(1.5.dp, PolishPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("share_map_image_button")
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = PolishPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COMPARTIR PLANO / EXPORTAR PNG",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Start new measurement
                TextButton(
                    onClick = onStartNew,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "+ Iniciar otra medición",
                        color = PolishPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        containerColor = PolishBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Metrics Banner
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, PolishCardBorder),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DISTANCIA TOTAL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextSecondary,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2f", state.totalDistanceMeters),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishPrimary
                            )
                            Text(
                                text = " m",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishPrimary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "(${state.totalDistanceCm} cm)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = PolishTextTertiary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        if (state.isPolygonClosed && state.markedPoints.size >= 3) {
                            HorizontalDivider(
                                color = PolishCardBorderLight,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Área Estimada", fontSize = 11.sp, color = PolishTextSecondary)
                                    Text(
                                        "${String.format(Locale.US, "%.2f", state.areaSquareMeters)} m²",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishTextPrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Perímetro", fontSize = 11.sp, color = PolishTextSecondary)
                                    Text(
                                        "${String.format(Locale.US, "%.2f", state.totalDistanceMeters)} m",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishTextPrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Vértices", fontSize = 11.sp, color = PolishTextSecondary)
                                    Text(
                                        "${state.markedPoints.size} Puntos",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Map Canvas Schematic Diagram
            item {
                Text(
                    text = "PLANO VECTORIAL GENERADO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishPrimary,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    MapCanvas(
                        markedPoints = state.markedPoints,
                        segments = state.segments,
                        gpsBreadcrumbs = state.gpsBreadcrumbs,
                        isPolygonClosed = state.isPolygonClosed,
                        isInteractive = true,
                        showDistanceLabels = true,
                        showBreadcrumbs = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Segment Breakdown Table Header
            item {
                Text(
                    text = "DESGLOSE DE DISTANCIAS ENTRE LETRAS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishPrimary,
                    letterSpacing = 0.5.sp
                )
            }

            // Segment Items List
            itemsIndexed(state.segments) { index, segment ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PolishCardBorder),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = PolishPrimaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, PolishPrimaryLight)
                            ) {
                                Text(
                                    text = "${segment.fromLetter} ➔ ${segment.toLetter}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishPrimaryDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Tramo ${index + 1}",
                                fontSize = 13.sp,
                                color = PolishTextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${String.format(Locale.US, "%.2f", segment.distanceMeters)} m",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = "${segment.distanceCm} cm",
                                fontSize = 12.sp,
                                color = PolishTextTertiary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

