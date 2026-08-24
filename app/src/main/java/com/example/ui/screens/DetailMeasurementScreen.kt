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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun DetailMeasurementScreen(
    record: MeasurementRecord,
    onShareImage: () -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val points = remember(record.pointsJson) { GeometryUtils.jsonToPoints(record.pointsJson) }
    val segments = remember(record.segmentsJson) { GeometryUtils.jsonToSegments(record.segmentsJson) }
    val dateStr = remember(record.timestamp) {
        SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(record.timestamp))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = record.title.uppercase(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = PolishTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { onDelete(record.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = AccentRed)
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onShareImage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PolishPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("detail_share_image_button")
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXPORTAR / COMPARTIR PLANO PNG",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
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
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, PolishCardBorder),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = dateStr,
                            fontSize = 12.sp,
                            color = PolishTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "DISTANCIA TOTAL",
                                    fontSize = 11.sp,
                                    color = PolishTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", record.totalDistanceMeters)} m",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishPrimary
                                )
                                Text(
                                    text = "(${record.totalDistanceCm} cm)",
                                    fontSize = 13.sp,
                                    color = PolishTextTertiary
                                )
                            }

                            if (record.isClosedPolygon && record.areaSquareMeters > 0) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "ÁREA ESTIMADA",
                                        fontSize = 11.sp,
                                        color = PolishTextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", record.areaSquareMeters)} m²",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishTextPrimary
                                    )
                                    Text(
                                        text = "${record.pointsCount} vértices",
                                        fontSize = 13.sp,
                                        color = PolishPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "MAPA Y PLANO DE RECORRIDO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishPrimary,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    MapCanvas(
                        markedPoints = points,
                        segments = segments,
                        isPolygonClosed = record.isClosedPolygon,
                        isInteractive = true,
                        showDistanceLabels = true,
                        showBreadcrumbs = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            item {
                Text(
                    text = "SECUENCIA DE PUNTOS Y COTAS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishPrimary,
                    letterSpacing = 0.5.sp
                )
            }

            itemsIndexed(segments) { index, segment ->
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

