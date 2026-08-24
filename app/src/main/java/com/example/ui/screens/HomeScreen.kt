package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    onStartNewMeasurement: () -> Unit,
    onStartSimulationMode: () -> Unit,
    onViewHistory: () -> Unit,
    historyCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PolishBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Brand & Logo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            // App Logo Icon Container
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape, spotColor = PolishPrimary)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "Logo GeoMeasure Pro",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "GeoMeasure Pro",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = PolishPrimary,
                letterSpacing = 0.25.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Medición GPS de Áreas, Polígonos y Rutas",
                fontSize = 14.sp,
                color = PolishTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = PolishPrimaryContainer,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, PolishPrimaryLight)
            ) {
                Text(
                    text = "Vértices A-Z  •  Metros & cm  •  Exportación PNG",
                    fontSize = 12.sp,
                    color = PolishPrimaryDark,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Middle Section: Feature Highlight Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureItem(
                icon = Icons.Default.GpsFixed,
                title = "Seguimiento en Tiempo Real",
                description = "El mapa sigue tu posición y dibuja la ruta mientras caminas."
            )
            FeatureItem(
                icon = Icons.Default.Straighten,
                title = "Registro de Distancias por Letras",
                description = "Marca puntos A, B, C y obtén el desglose exacto de A-B, B-C en m y cm."
            )
            FeatureItem(
                icon = Icons.Default.Share,
                title = "Generación y Compartición de Planos",
                description = "Exporta el mapa con cotas y áreas directamente como imagen PNG."
            )
        }

        // Bottom Section: Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Main Start Button
            Button(
                onClick = onStartNewMeasurement,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PolishPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_new_measurement_button")
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "INICIAR NUEVA MEDICIÓN",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Simulation / Virtual Test Mode Button
            OutlinedButton(
                onClick = onStartSimulationMode,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = PolishSurfaceVariant,
                    contentColor = PolishPrimary
                ),
                border = BorderStroke(1.dp, PolishCardBorder),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_simulation_button")
            ) {
                Icon(
                    Icons.Default.DirectionsWalk,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = PolishPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MODO SIMULACIÓN / CAMINAR VIRTUAL",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // History Button
            OutlinedButton(
                onClick = onViewHistory,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = PolishTextPrimary
                ),
                border = BorderStroke(1.dp, PolishCardBorder),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("view_history_button")
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = PolishTextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (historyCount > 0) "HISTORIAL DE MEDICIONES ($historyCount)" else "HISTORIAL DE MEDICIONES",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, PolishCardBorder),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PolishPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PolishPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = PolishTextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

