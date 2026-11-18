package com.sonosequencer.alternative.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeakerDao {
    
    @Query("SELECT * FROM speakers ORDER BY name ASC")
    fun getAllSpeakers(): Flow<List<Speaker>>
    
    @Query("SELECT * FROM speakers WHERE id = :id")
    suspend fun getSpeakerById(id: String): Speaker?
    
    @Query("SELECT * FROM speakers WHERE roomId = :roomId ORDER BY name ASC")
    fun getSpeakersByRoom(roomId: String): Flow<List<Speaker>>
    
    @Query("SELECT * FROM speakers WHERE isConnected = 1 ORDER BY name ASC")
    fun getConnectedSpeakers(): Flow<List<Speaker>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeaker(speaker: Speaker)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeakers(speakers: List<Speaker>)
    
    @Update
    suspend fun updateSpeaker(speaker: Speaker)
    
    @Delete
    suspend fun deleteSpeaker(speaker: Speaker)
    
    @Query("DELETE FROM speakers WHERE id = :id")
    suspend fun deleteSpeakerById(id: String)
    
    @Query("DELETE FROM speakers")
    suspend fun deleteAllSpeakers()
    
    @Query("UPDATE speakers SET isConnected = :isConnected WHERE id = :id")
    suspend fun updateConnectionStatus(id: String, isConnected: Boolean)
    
    @Query("UPDATE speakers SET heightCm = :height, distanceFromWallCm = :distanceFromWall, distanceFromCornerCm = :distanceFromCorner, angleDegrees = :angle WHERE id = :id")
    suspend fun updateSpeakerPosition(
        id: String,
        height: Double,
        distanceFromWall: Double,
        distanceFromCorner: Double,
        angle: Double
    )
}
