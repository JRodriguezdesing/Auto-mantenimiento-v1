package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Vehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY brand ASC, model ASC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE clientId = :clientId ORDER BY brand ASC")
    fun getVehiclesByClient(clientId: Long): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getVehicleById(id: Long): Vehicle?

    @Query("SELECT * FROM vehicles WHERE plateNumber = :plate LIMIT 1")
    suspend fun getVehicleByPlate(plate: String): Vehicle?

    @Query("SELECT * FROM vehicles WHERE plateNumber LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR model LIKE '%' || :query || '%'")
    fun searchVehicles(query: String): Flow<List<Vehicle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Query("UPDATE vehicles SET currentMileage = :newMileage, updatedAt = :timestamp WHERE id = :vehicleId")
    suspend fun updateMileage(vehicleId: Long, newMileage: Int, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("SELECT COUNT(*) FROM vehicles")
    fun getVehicleCount(): Flow<Int>
}
