package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ServiceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceRecordDao {
    @Query("SELECT * FROM service_records ORDER BY serviceDate DESC")
    fun getAllServices(): Flow<List<ServiceRecord>>

    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId ORDER BY serviceDate DESC")
    fun getServicesByVehicle(vehicleId: Long): Flow<List<ServiceRecord>>

    @Query("SELECT * FROM service_records WHERE clientId = :clientId ORDER BY serviceDate DESC")
    fun getServicesByClient(clientId: Long): Flow<List<ServiceRecord>>

    @Query("SELECT * FROM service_records WHERE id = :id LIMIT 1")
    suspend fun getServiceById(id: Long): ServiceRecord?

    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId ORDER BY serviceDate DESC LIMIT 1")
    suspend fun getLatestServiceForVehicle(vehicleId: Long): ServiceRecord?

    @Query("SELECT * FROM service_records WHERE isSyncedToSheets = 0")
    suspend fun getUnsyncedServices(): List<ServiceRecord>

    @Query("UPDATE service_records SET isSyncedToSheets = 1 WHERE id IN (:ids)")
    suspend fun markServicesSynced(ids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: ServiceRecord): Long

    @Update
    suspend fun updateService(service: ServiceRecord)

    @Delete
    suspend fun deleteService(service: ServiceRecord)

    @Query("SELECT COUNT(*) FROM service_records")
    fun getServiceCount(): Flow<Int>

    @Query("SELECT SUM(totalCost) FROM service_records WHERE serviceDate >= :sinceTimestamp")
    fun getTotalRevenueSince(sinceTimestamp: Long): Flow<Double?>
}
