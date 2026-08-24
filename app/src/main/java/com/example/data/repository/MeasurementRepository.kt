package com.example.data.repository

import com.example.data.db.MeasurementDao
import com.example.data.model.MeasurementRecord
import kotlinx.coroutines.flow.Flow

class MeasurementRepository(private val dao: MeasurementDao) {
    val allMeasurements: Flow<List<MeasurementRecord>> = dao.getAllMeasurements()

    suspend fun getMeasurementById(id: Long): MeasurementRecord? = dao.getMeasurementById(id)

    suspend fun insertMeasurement(record: MeasurementRecord): Long = dao.insertMeasurement(record)

    suspend fun deleteMeasurement(id: Long) = dao.deleteMeasurement(id)

    suspend fun clearAll() = dao.clearAll()
}
