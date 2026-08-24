package com.example.ui.components

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GeoPoint
import com.example.data.model.LocationPoint
import com.example.data.model.SegmentMeasure
import com.example.ui.theme.*
import com.example.util.GeometryUtils
import java.util.Locale
import kotlin.math.*

@Composable
fun MapCanvas(
    markedPoints: List<GeoPoint>,
    segments: List<SegmentMeasure>,
    gpsBreadcrumbs: List<LocationPoint> = emptyList(),
    currentLocation: android.location.Location? = null,
    bearing: Float? = null,
    isPolygonClosed: Boolean = false,
    isInteractive: Boolean = true,
    showDistanceLabels: Boolean = true,
    showBreadcrumbs: Boolean = true,
    onMapTap: ((lat: Double, lon: Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF3EDF7))
            .border(BorderStroke(1.dp, Color(0xFFCAC4D0)), RoundedCornerShape(24.dp))
            .pointerInput(isInteractive) {
                if (isInteractive) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.2f, 8.0f)
                        panOffset += pan
                    }
                }
            }
            .pointerInput(isInteractive, onMapTap) {
                if (isInteractive && onMapTap != null) {
                    detectTapGestures { offset ->
                    }
                }
            }
            .testTag("map_canvas_box")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 1. Draw Polish Dot Matrix / Blueprint Grid
            drawBlueprintGrid(canvasWidth, canvasHeight, panOffset, zoomScale)

            // Collect all reference points to compute bounds
            val allPts = mutableListOf<Pair<Double, Double>>()
            if (markedPoints.isNotEmpty()) {
                val projected = GeometryUtils.projectPoints(markedPoints)
                allPts.addAll(projected)
            } else if (currentLocation != null) {
                allPts.add(Pair(0.0, 0.0))
            }

            val originLat = markedPoints.firstOrNull()?.latitude ?: currentLocation?.latitude ?: 0.0
            val originLon = markedPoints.firstOrNull()?.longitude ?: currentLocation?.longitude ?: 0.0
            val latRad = Math.toRadians(originLat)
            val metersPerDegreeLat = 111132.954
            val metersPerDegreeLon = 111412.84 * cos(latRad)

            // Convert current location to metric coordinates relative to origin
            val userMetricPos = currentLocation?.let { loc ->
                val ux = (loc.longitude - originLon) * metersPerDegreeLon
                val uy = (loc.latitude - originLat) * metersPerDegreeLat
                Pair(ux, uy)
            }

            if (userMetricPos != null && markedPoints.isEmpty()) {
                allPts.add(userMetricPos)
            }

            // Determine bounds
            val minX = allPts.minOfOrNull { it.first } ?: -10.0
            val maxX = allPts.maxOfOrNull { it.first } ?: 10.0
            val minY = allPts.minOfOrNull { it.second } ?: -10.0
            val maxY = allPts.maxOfOrNull { it.second } ?: 10.0

            val spanX = max(maxX - minX, 12.0)
            val spanY = max(maxY - minY, 12.0)

            val padding = 70f
            val baseScale = min((canvasWidth - padding * 2) / spanX, (canvasHeight - padding * 2) / spanY).toFloat()
            val effectiveScale = baseScale * zoomScale

            val centerX = canvasWidth / 2f + panOffset.x
            val centerY = canvasHeight / 2f + panOffset.y
            val centerMetricX = (minX + maxX) / 2.0
            val centerMetricY = (minY + maxY) / 2.0

            fun metricToScreen(mx: Double, my: Double): Offset {
                val sx = centerX + ((mx - centerMetricX) * effectiveScale).toFloat()
                // Invert Y axis (metric +Y is North, Screen +Y is Down)
                val sy = centerY - ((my - centerMetricY) * effectiveScale).toFloat()
                return Offset(sx, sy)
            }

            // 2. Draw Walked Breadcrumbs Trail
            if (showBreadcrumbs && gpsBreadcrumbs.size >= 2) {
                val breadcrumbPath = Path()
                var first = true
                for (b in gpsBreadcrumbs) {
                    val bx = (b.longitude - originLon) * metersPerDegreeLon
                    val by = (b.latitude - originLat) * metersPerDegreeLat
                    val screenPt = metricToScreen(bx, by)
                    if (first) {
                        breadcrumbPath.moveTo(screenPt.x, screenPt.y)
                        first = false
                    } else {
                        breadcrumbPath.lineTo(screenPt.x, screenPt.y)
                    }
                }
                drawPath(
                    path = breadcrumbPath,
                    color = Color(0x666750A4),
                    style = Stroke(
                        width = 3.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )
                )
            }

            // 3. Project and Draw Polygon / Segments
            if (markedPoints.isNotEmpty()) {
                val projected = GeometryUtils.projectPoints(markedPoints)
                val screenPoints = projected.map { (px, py) -> metricToScreen(px, py) }

                // Polygon Fill
                if (isPolygonClosed && screenPoints.size >= 3) {
                    val polyPath = Path()
                    polyPath.moveTo(screenPoints[0].x, screenPoints[0].y)
                    for (i in 1 until screenPoints.size) {
                        polyPath.lineTo(screenPoints[i].x, screenPoints[i].y)
                    }
                    polyPath.close()

                    drawPath(
                        path = polyPath,
                        color = Color(0x286750A4)
                    )
                }

                // Clean Purple Segments
                val numSegments = if (isPolygonClosed && screenPoints.size >= 3) screenPoints.size else screenPoints.size - 1
                for (i in 0 until numSegments) {
                    val p1 = screenPoints[i]
                    val p2 = screenPoints[(i + 1) % screenPoints.size]

                    // Subtle background highlight
                    drawLine(
                        color = Color(0x226750A4),
                        start = p1,
                        end = p2,
                        strokeWidth = 10f,
                        cap = StrokeCap.Round
                    )
                    // Core line (Professional Polish purple dash/solid)
                    drawLine(
                        color = PolishPrimary,
                        start = p1,
                        end = p2,
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }

                // Segments Distance Badges
                if (showDistanceLabels) {
                    drawIntoCanvas { canvas ->
                        val paintBg = AndroidPaint().apply {
                            color = android.graphics.Color.WHITE
                            style = AndroidPaint.Style.FILL
                            isAntiAlias = true
                        }
                        val paintBorder = AndroidPaint().apply {
                            color = android.graphics.Color.parseColor("#CAC4D0")
                            style = AndroidPaint.Style.STROKE
                            strokeWidth = 2f
                            isAntiAlias = true
                        }
                        val paintText = AndroidPaint().apply {
                            color = android.graphics.Color.parseColor("#49454F")
                            textSize = 30f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            textAlign = AndroidPaint.Align.CENTER
                            isAntiAlias = true
                        }

                        for (i in 0 until numSegments) {
                            val p1 = screenPoints[i]
                            val p2 = screenPoints[(i + 1) % screenPoints.size]
                            val seg = segments.getOrNull(i)
                            val label = if (seg != null) {
                                "${seg.fromLetter}-${seg.toLetter}: ${String.format(Locale.US, "%.1fm", seg.distanceMeters)}"
                            } else ""

                            if (label.isNotEmpty()) {
                                val midX = (p1.x + p2.x) / 2
                                val midY = (p1.y + p2.y) / 2
                                val textWidth = paintText.measureText(label)
                                val rect = android.graphics.RectF(
                                    midX - textWidth / 2 - 14,
                                    midY - 22,
                                    midX + textWidth / 2 + 14,
                                    midY + 22
                                )
                                canvas.nativeCanvas.drawRoundRect(rect, 10f, 10f, paintBg)
                                canvas.nativeCanvas.drawRoundRect(rect, 10f, 10f, paintBorder)
                                canvas.nativeCanvas.drawText(label, midX, midY + 10, paintText)
                            }
                        }
                    }
                }

                // Draw Vertex Nodes (A, B, C, D, ...)
                drawIntoCanvas { canvas ->
                    val nodeRadius = 30f
                    val paintGlow = AndroidPaint().apply {
                        color = android.graphics.Color.parseColor("#336750A4")
                        isAntiAlias = true
                    }
                    val paintNodePrimary = AndroidPaint().apply {
                        color = android.graphics.Color.parseColor("#6750A4")
                        isAntiAlias = true
                    }
                    val paintNodeAccent = AndroidPaint().apply {
                        color = android.graphics.Color.parseColor("#B3261E")
                        isAntiAlias = true
                    }
                    val paintInner = AndroidPaint().apply {
                        color = android.graphics.Color.WHITE
                        isAntiAlias = true
                    }
                    val paintLetter = AndroidPaint().apply {
                        color = android.graphics.Color.parseColor("#1D1B20")
                        textSize = 32f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = AndroidPaint.Align.CENTER
                        isAntiAlias = true
                    }

                    for (i in markedPoints.indices) {
                        val pt = screenPoints[i]
                        val letter = markedPoints[i].letter
                        val isLastPoint = i == markedPoints.lastIndex

                        val activePaint = if (isLastPoint) paintNodeAccent else paintNodePrimary
                        canvas.nativeCanvas.drawCircle(pt.x, pt.y, nodeRadius + 6, paintGlow)
                        canvas.nativeCanvas.drawCircle(pt.x, pt.y, nodeRadius, activePaint)
                        canvas.nativeCanvas.drawCircle(pt.x, pt.y, nodeRadius - 5, paintInner)
                        canvas.nativeCanvas.drawText(letter, pt.x, pt.y + 11, paintLetter)
                    }
                }
            }

            // 4. Draw Live User Location Pointer
            if (userMetricPos != null) {
                val userScreen = metricToScreen(userMetricPos.first, userMetricPos.second)

                // Pulse Halo
                drawCircle(
                    color = Color(0x336750A4),
                    radius = 36f,
                    center = userScreen
                )
                drawCircle(
                    color = PolishPrimary,
                    radius = 16f,
                    center = userScreen
                )
                drawCircle(
                    color = Color.White,
                    radius = 7f,
                    center = userScreen
                )

                // Direction Arrow
                val angle = bearing ?: 0f
                rotate(degrees = angle, pivot = userScreen) {
                    val arrowPath = Path().apply {
                        moveTo(userScreen.x, userScreen.y - 28f)
                        lineTo(userScreen.x - 10f, userScreen.y - 12f)
                        lineTo(userScreen.x + 10f, userScreen.y - 12f)
                        close()
                    }
                    drawPath(
                        path = arrowPath,
                        color = PolishPrimary
                    )
                }

                // If currently walking away from last marked point, draw connection line to live position
                if (markedPoints.isNotEmpty()) {
                    val lastProjected = GeometryUtils.projectPoints(markedPoints).last()
                    val lastScreen = metricToScreen(lastProjected.first, lastProjected.second)
                    drawLine(
                        color = Color(0x996750A4),
                        start = lastScreen,
                        end = userScreen,
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
            }
        }

        // Overlay Controls (Zoom In, Zoom Out, Center)
        if (isInteractive) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { zoomScale = (zoomScale * 1.3f).coerceAtMost(8.0f) },
                    containerColor = Color.White,
                    contentColor = PolishPrimary,
                    shape = RoundedCornerShape(12.dp),
                    elevation = FloatingActionButtonDefaults.elevation(2.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Acercar zoom")
                }
                SmallFloatingActionButton(
                    onClick = { zoomScale = (zoomScale / 1.3f).coerceAtLeast(0.3f) },
                    containerColor = Color.White,
                    contentColor = PolishPrimary,
                    shape = RoundedCornerShape(12.dp),
                    elevation = FloatingActionButtonDefaults.elevation(2.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Alejar zoom")
                }
                SmallFloatingActionButton(
                    onClick = {
                        panOffset = Offset.Zero
                        zoomScale = 1.0f
                    },
                    containerColor = Color.White,
                    contentColor = PolishPrimary,
                    shape = RoundedCornerShape(12.dp),
                    elevation = FloatingActionButtonDefaults.elevation(2.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Centrar mapa")
                }
            }
        }

        // Compass Rose indicator in top left
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, Color(0xFFCAC4D0)), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Navigation,
                contentDescription = "Norte",
                tint = AccentRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "N",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Scale / Points List Overlay Badge (Bottom Right)
        Surface(
            color = Color.White.copy(alpha = 0.92f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (markedPoints.isNotEmpty()) "Puntos: ${markedPoints.size} (${markedPoints.first().letter}-${markedPoints.last().letter})" else "Escala Dinámica",
                    color = PolishTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun DrawScope.drawBlueprintGrid(
    width: Float,
    height: Float,
    panOffset: Offset,
    zoomScale: Float
) {
    val baseSpacing = 32f * zoomScale
    val dotColor = Color(0xFFCAC4D0).copy(alpha = 0.6f)

    var x = (panOffset.x % baseSpacing)
    while (x < width) {
        var y = (panOffset.y % baseSpacing)
        while (y < height) {
            drawCircle(
                color = dotColor,
                radius = 1.5f,
                center = Offset(x, y)
            )
            y += baseSpacing
        }
        x += baseSpacing
    }
}

