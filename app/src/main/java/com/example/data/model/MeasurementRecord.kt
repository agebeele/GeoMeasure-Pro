package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class MeasurementRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val totalDistanceMeters: Double,
    val totalDistanceCm: Long,
    val areaSquareMeters: Double,
    val isClosedPolygon: Boolean,
    val pointsCount: Int,
    val pointsJson: String,
    val segmentsJson: String
)
