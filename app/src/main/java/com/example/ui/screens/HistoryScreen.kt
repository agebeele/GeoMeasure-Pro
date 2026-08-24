package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MeasurementRecord
import com.example.ui.components.MapCanvas
import com.example.ui.theme.*
import com.example.util.GeometryUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    records: List<MeasurementRecord>,
    onSelectRecord: (MeasurementRecord) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onBack: () -> Unit,
    onStartNew: () -> Unit,
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
                            Icons.Default.History,
                            contentDescription = null,
                            tint = PolishPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "HISTORIAL DE MEDICIONES",
                            fontSize = 16.sp,
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PolishBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartNew,
                containerColor = PolishPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva medición")
            }
        },
        containerColor = PolishBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(PolishPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            tint = PolishPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = "No hay mediciones guardadas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )
                    Text(
                        text = "Inicia un recorrido GPS para medir distancias entre puntos A, B, C y calcular áreas.",
                        fontSize = 14.sp,
                        color = PolishTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = onStartNew,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PolishPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Iniciar Medición", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    HistoryItemCard(
                        record = record,
                        onClick = { onSelectRecord(record) },
                        onDelete = { onDeleteRecord(record.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    record: MeasurementRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val points = remember(record.pointsJson) { GeometryUtils.jsonToPoints(record.pointsJson) }
    val segments = remember(record.segmentsJson) { GeometryUtils.jsonToSegments(record.segmentsJson) }
    val dateStr = remember(record.timestamp) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(record.timestamp))
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar medición?") },
            text = { Text("Se eliminará definitivamente el registro '${record.title}'.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("Eliminar", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = PolishPrimary)
                }
            },
            containerColor = Color.White,
            titleContentColor = PolishTextPrimary,
            textContentColor = PolishTextSecondary
        )
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PolishCardBorder),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("history_item_${record.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniature Route Thumbnail Canvas
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PolishMapBg)
                    .border(BorderStroke(1.dp, PolishCardBorder), RoundedCornerShape(12.dp))
            ) {
                MapCanvas(
                    markedPoints = points,
                    segments = segments,
                    isPolygonClosed = record.isClosedPolygon,
                    isInteractive = false,
                    showDistanceLabels = false,
                    showBreadcrumbs = false,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = record.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishPrimary
                    )
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar",
                            tint = PolishTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = PolishTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateStr,
                        fontSize = 12.sp,
                        color = PolishTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Distancia: ${String.format(Locale.US, "%.2f", record.totalDistanceMeters)} m",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary
                        )
                        if (record.isClosedPolygon && record.areaSquareMeters > 0) {
                            Text(
                                text = "Área: ${String.format(Locale.US, "%.2f", record.areaSquareMeters)} m²",
                                fontSize = 12.sp,
                                color = PolishPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "${record.pointsCount} Puntos (${points.take(4).joinToString("") { it.letter }}...)",
                                fontSize = 12.sp,
                                color = PolishTextSecondary
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = PolishPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

