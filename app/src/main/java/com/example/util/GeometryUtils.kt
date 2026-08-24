package com.example.util

import com.example.data.model.GeoPoint
import com.example.data.model.SegmentMeasure
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

object GeometryUtils {

    /**
     * Converts a 0-based index to an alphabetical label (0 -> "A", 1 -> "B", ..., 25 -> "Z", 26 -> "AA", etc.)
     */
    fun indexToLetter(index: Int): String {
        var num = index
        val sb = StringBuilder()
        while (num >= 0) {
            val rem = num % 26
            sb.insert(0, ('A'.code + rem).toChar())
            num = (num / 26) - 1
        }
        return sb.toString()
    }

    /**
     * Haversine distance between two GPS coordinates in meters.
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    fun metersToCm(meters: Double): Long {
        return (meters * 100.0).roundToLong()
    }

    /**
     * Calculates the area of a closed polygon formed by GeoPoints in square meters (m²)
     * using the Shoelace formula projected to local metric coordinates.
     */
    fun calculatePolygonArea(points: List<GeoPoint>): Double {
        if (points.size < 3) return 0.0

        val lat0 = points[0].latitude
        val lon0 = points[0].longitude
        val lat0Rad = Math.toRadians(lat0)
        val metersPerDegreeLat = 111132.954 - 559.822 * cos(2 * lat0Rad) + 1.175 * cos(4 * lat0Rad)
        val metersPerDegreeLon = 111412.84 * cos(lat0Rad) - 93.5 * cos(3 * lat0Rad)

        val projected = points.map { p ->
            val x = (p.longitude - lon0) * metersPerDegreeLon
            val y = (p.latitude - lat0) * metersPerDegreeLat
            Pair(x, y)
        }

        var area = 0.0
        val n = projected.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += projected[i].first * projected[j].second
            area -= projected[j].first * projected[i].second
        }

        return abs(area) / 2.0
    }

    /**
     * Projects GPS points into 2D Cartesian metric (X, Y) relative coordinates centered at origin.
     */
    fun projectPoints(points: List<GeoPoint>): List<Pair<Double, Double>> {
        if (points.isEmpty()) return emptyList()
        val avgLat = points.map { it.latitude }.average()
        val avgLon = points.map { it.longitude }.average()
        val latRad = Math.toRadians(avgLat)
        val metersPerDegreeLat = 111132.954 - 559.822 * cos(2 * latRad) + 1.175 * cos(4 * latRad)
        val metersPerDegreeLon = 111412.84 * cos(latRad) - 93.5 * cos(3 * latRad)

        return points.map { p ->
            val x = (p.longitude - avgLon) * metersPerDegreeLon
            val y = (p.latitude - avgLat) * metersPerDegreeLat
            Pair(x, y)
        }
    }

    /**
     * Constructs segment measures between consecutive points and optionally closes the loop.
     */
    fun buildSegments(points: List<GeoPoint>, isClosed: Boolean): List<SegmentMeasure> {
        if (points.size < 2) return emptyList()
        val segments = mutableListOf<SegmentMeasure>()
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val dist = calculateDistance(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
            segments.add(
                SegmentMeasure(
                    fromLetter = p1.letter,
                    toLetter = p2.letter,
                    distanceMeters = dist,
                    distanceCm = metersToCm(dist)
                )
            )
        }
        if (isClosed && points.size >= 3) {
            val last = points.last()
            val first = points.first()
            val dist = calculateDistance(last.latitude, last.longitude, first.latitude, first.longitude)
            segments.add(
                SegmentMeasure(
                    fromLetter = last.letter,
                    toLetter = first.letter,
                    distanceMeters = dist,
                    distanceCm = metersToCm(dist)
                )
            )
        }
        return segments
    }

    // JSON serialization helpers
    fun pointsToJson(points: List<GeoPoint>): String {
        val array = JSONArray()
        for (p in points) {
            val obj = JSONObject().apply {
                put("letter", p.letter)
                put("latitude", p.latitude)
                put("longitude", p.longitude)
                put("distanceFromPrevMeters", p.distanceFromPrevMeters)
                put("distanceFromPrevCm", p.distanceFromPrevCm)
                put("timestamp", p.timestamp)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToPoints(json: String): List<GeoPoint> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<GeoPoint>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                GeoPoint(
                    letter = obj.getString("letter"),
                    latitude = obj.getDouble("latitude"),
                    longitude = obj.getDouble("longitude"),
                    distanceFromPrevMeters = obj.optDouble("distanceFromPrevMeters", 0.0),
                    distanceFromPrevCm = obj.optLong("distanceFromPrevCm", 0L),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }
        return list
    }

    fun segmentsToJson(segments: List<SegmentMeasure>): String {
        val array = JSONArray()
        for (s in segments) {
            val obj = JSONObject().apply {
                put("fromLetter", s.fromLetter)
                put("toLetter", s.toLetter)
                put("distanceMeters", s.distanceMeters)
                put("distanceCm", s.distanceCm)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToSegments(json: String): List<SegmentMeasure> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<SegmentMeasure>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                SegmentMeasure(
                    fromLetter = obj.getString("fromLetter"),
                    toLetter = obj.getString("toLetter"),
                    distanceMeters = obj.getDouble("distanceMeters"),
                    distanceCm = obj.getLong("distanceCm")
                )
            )
        }
        return list
    }
}
