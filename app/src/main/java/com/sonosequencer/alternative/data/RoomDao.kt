package com.sonosequencer.alternative.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    
    @Query("SELECT * FROM Room ORDER BY name ASC")
    fun getAllRooms(): Flow<List<Room>>
    
    @Query("SELECT * FROM Room WHERE id = :id")
    suspend fun getRoomById(id: String): Room?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: Room)
    
    @Update
    suspend fun updateRoom(room: Room)
    
    @Delete
    suspend fun deleteRoom(room: Room)
    
    @Query("DELETE FROM Room WHERE id = :id")
    suspend fun deleteRoomById(id: String)
    
    @Query("DELETE FROM Room")
    suspend fun deleteAllRooms()
}
