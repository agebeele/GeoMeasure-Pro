package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.core.content.FileProvider
import com.example.data.model.GeoPoint
import com.example.data.model.SegmentMeasure
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

object ImageExportUtils {

    fun generateMeasurementBitmap(
        title: String,
        timestamp: Long,
        totalDistanceMeters: Double,
        areaSquareMeters: Double,
        isClosed: Boolean,
        points: List<GeoPoint>,
        segments: List<SegmentMeasure>
    ): Bitmap {
        val width = 1080
        val height = 1440
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background: Professional Polish Canvas
        val bgPaint = Paint().apply {
            color = Color.parseColor("#F7F2FA")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Header Background Banner
        val headerBannerPaint = Paint().apply {
            color = Color.parseColor("#6750A4")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 140f, headerBannerPaint)

        // Header Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("GEOMEASURE PRO", 60f, 68f, titlePaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EADDFF")
            textSize = 22f
            typeface = Typeface.DEFAULT
        }
        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))
        canvas.drawText("$title  •  $dateStr", 60f, 108f, subtitlePaint)

        // Stats Banner Card
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(8f, 0f, 4f, Color.parseColor("#20000000"))
        }
        val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CAC4D0")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val cardRect = RectF(60f, 170f, width - 60f, 290f)
        canvas.drawRoundRect(cardRect, 20f, 20f, cardPaint)
        canvas.drawRoundRect(cardRect, 20f, 20f, cardBorder)

        // Stat 1: Total Distance
        val statLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#49454F")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val statValuePurple = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6750A4")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val statValueDark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D1B20")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("DISTANCIA TOTAL", 90f, 215f, statLabelPaint)
        val distText = String.format(Locale.US, "%.2f m", totalDistanceMeters)
        val distCmText = " (${(totalDistanceMeters * 100).toLong()} cm)"
        canvas.drawText(distText + distCmText, 90f, 260f, statValuePurple)

        // Stat 2: Area
        val col2X = 620f
        canvas.drawText("ÁREA ESTIMADA", col2X, 215f, statLabelPaint)
        val areaText = if (isClosed && points.size >= 3) {
            String.format(Locale.US, "%.2f m²", areaSquareMeters)
        } else {
            "Ruta Abierta"
        }
        canvas.drawText(areaText, col2X, 260f, statValueDark)

        // Map Canvas Box
        val mapBox = RectF(60f, 320f, width - 60f, 980f)
        val mapBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EDE7F6")
            style = Paint.Style.FILL
        }
        val mapBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D0BCFF")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(mapBox, 24f, 24f, mapBgPaint)

        // Subtle Map Grid
        val gridPaint = Paint().apply {
            color = Color.parseColor("#E0D6ED")
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }
        val gridSize = 60f
        var gx = mapBox.left
        while (gx < mapBox.right) {
            canvas.drawLine(gx, mapBox.top, gx, mapBox.bottom, gridPaint)
            gx += gridSize
        }
        var gy = mapBox.top
        while (gy < mapBox.bottom) {
            canvas.drawLine(mapBox.left, gy, mapBox.right, gy, gridPaint)
            gy += gridSize
        }
        canvas.drawRoundRect(mapBox, 24f, 24f, mapBorderPaint)

        // Compass Rose indicator in map
        val compassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6750A4")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("N ↑", mapBox.right - 60f, mapBox.top + 45f, compassPaint)

        // Project and Render Points on Map
        if (points.isNotEmpty()) {
            val projected = GeometryUtils.projectPoints(points)
            val minX = projected.minOf { it.first }
            val maxX = projected.maxOf { it.first }
            val minY = projected.minOf { it.second }
            val maxY = projected.maxOf { it.second }

            val spanX = max(maxX - minX, 1.0)
            val spanY = max(maxY - minY, 1.0)

            val innerPadding = 90f
            val drawWidth = mapBox.width() - (innerPadding * 2)
            val drawHeight = mapBox.height() - (innerPadding * 2)

            val scale = min(drawWidth / spanX, drawHeight / spanY)

            val screenCoords = projected.map { (x, y) ->
                val sx = (mapBox.left + innerPadding + ((x - minX) * scale) + (drawWidth - (spanX * scale)) / 2).toFloat()
                // Invert Y because metric Y goes North (+up), while canvas Y goes down
                val sy = (mapBox.bottom - innerPadding - ((y - minY) * scale) - (drawHeight - (spanY * scale)) / 2).toFloat()
                PointF(sx, sy)
            }

            // Fill Polygon Area if closed
            if (isClosed && screenCoords.size >= 3) {
                val fillPath = Path()
                fillPath.moveTo(screenCoords[0].x, screenCoords[0].y)
                for (i in 1 until screenCoords.size) {
                    fillPath.lineTo(screenCoords[i].x, screenCoords[i].y)
                }
                fillPath.close()

                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#336750A4")
                    style = Paint.Style.FILL
                }
                canvas.drawPath(fillPath, fillPaint)
            }

            // Draw Lines
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#6750A4")
                strokeWidth = 5f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#336750A4")
                strokeWidth = 12f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }

            val numSegments = if (isClosed && screenCoords.size >= 3) screenCoords.size else screenCoords.size - 1
            for (i in 0 until numSegments) {
                val p1 = screenCoords[i]
                val p2 = screenCoords[(i + 1) % screenCoords.size]
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, glowPaint)
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, linePaint)
            }

            // Draw Distance Badges on segments
            val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            val badgeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#6750A4")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            val badgeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#6750A4")
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            for (i in 0 until numSegments) {
                val p1 = screenCoords[i]
                val p2 = screenCoords[(i + 1) % screenCoords.size]
                val seg = segments.getOrNull(i)
                val label = if (seg != null) {
                    "${seg.fromLetter}-${seg.toLetter}: " + String.format(Locale.US, "%.1fm", seg.distanceMeters)
                } else {
                    ""
                }

                if (label.isNotEmpty()) {
                    val midX = (p1.x + p2.x) / 2
                    val midY = (p1.y + p2.y) / 2
                    val textWidth = badgeText.measureText(label)
                    val bRect = RectF(midX - textWidth / 2 - 12, midY - 18, midX + textWidth / 2 + 12, midY + 18)
                    canvas.drawRoundRect(bRect, 8f, 8f, badgeBg)
                    canvas.drawRoundRect(bRect, 8f, 8f, badgeBorder)
                    canvas.drawText(label, midX, midY + 6, badgeText)
                }
            }

            // Draw Node Points (A, B, C...)
            val nodeRadius = 26f
            val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#6750A4")
                style = Paint.Style.FILL
            }
            val nodeInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#4F378B")
                style = Paint.Style.FILL
            }
            val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            for (i in points.indices) {
                val pt = screenCoords[i]
                canvas.drawCircle(pt.x, pt.y, nodeRadius + 4, glowPaint)
                canvas.drawCircle(pt.x, pt.y, nodeRadius, nodePaint)
                canvas.drawCircle(pt.x, pt.y, nodeRadius - 4, nodeInner)
                canvas.drawText(points[i].letter, pt.x, pt.y + 9, letterPaint)
            }
        }

        // Bottom Breakdown Section (Segment detail list)
        val tableTop = 1010f
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6750A4")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("REGISTRO DETALLADO DE SEGMENTOS", 60f, tableTop, tableHeaderPaint)

        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D1B20")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rowPurple = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6750A4")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rowDim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#79747E")
            textSize = 19f
        }

        val curY = tableTop + 40f
        val maxRows = min(segments.size, 8)
        for (i in 0 until maxRows) {
            val seg = segments[i]
            val leftCol = if (i < 4) 60f else (width / 2f + 20f)
            val yPos = if (i < 4) curY + (i * 45f) else curY + ((i - 4) * 45f)

            canvas.drawText("[ ${seg.fromLetter} ➔ ${seg.toLetter} ]", leftCol, yPos, rowPurple)
            val distStr = String.format(Locale.US, "%.2f m", seg.distanceMeters)
            val cmStr = " (${seg.distanceCm} cm)"
            canvas.drawText(distStr, leftCol + 130f, yPos, rowPaint)
            canvas.drawText(cmStr, leftCol + 230f, yPos, rowDim)
        }

        // Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#79747E")
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("GeoMeasure Pro • Sistema de Medición GPS de Áreas y Rutas", width / 2f, height - 30f, footerPaint)

        return bitmap
    }

    fun shareMeasurementImage(
        context: Context,
        title: String,
        timestamp: Long,
        totalDistanceMeters: Double,
        areaSquareMeters: Double,
        isClosed: Boolean,
        points: List<GeoPoint>,
        segments: List<SegmentMeasure>
    ) {
        try {
            val bitmap = generateMeasurementBitmap(
                title, timestamp, totalDistanceMeters, areaSquareMeters, isClosed, points, segments
            )
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "geomeasure_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Medición $title - GeoMeasure Pro")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Medición $title:\nDistancia total: ${String.format(Locale.US, "%.2f", totalDistanceMeters)} m (${(totalDistanceMeters * 100).toLong()} cm)\n" +
                            if (isClosed) "Área: ${String.format(Locale.US, "%.2f", areaSquareMeters)} m²\n" else "" +
                            "Puntos: ${points.joinToString(" -> ") { it.letter }}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Compartir Mapa de Medición")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

