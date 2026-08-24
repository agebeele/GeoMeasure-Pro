package com.example.data.model

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

data class GeoPoint(
    val letter: String, // "A", "B", "C", ...
    val latitude: Double,
    val longitude: Double,
    val distanceFromPrevMeters: Double = 0.0,
    val distanceFromPrevCm: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

data class SegmentMeasure(
    val fromLetter: String, // "A"
    val toLetter: String,   // "B"
    val distanceMeters: Double,
    val distanceCm: Long
)
