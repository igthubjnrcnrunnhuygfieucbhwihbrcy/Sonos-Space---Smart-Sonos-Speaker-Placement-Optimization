package com.sonosequencer.alternative.data

import kotlinx.coroutines.flow.Flow

class SpeakerRepository(private val speakerDao: SpeakerDao) {
    
    val allSpeakers: Flow<List<Speaker>> = speakerDao.getAllSpeakers()
    
    suspend fun getSpeakerById(id: String): Speaker? {
        return speakerDao.getSpeakerById(id)
    }
    
    suspend fun getSpeakersByRoom(roomId: String): Flow<List<Speaker>> {
        return speakerDao.getSpeakersByRoom(roomId)
    }
    
    suspend fun getConnectedSpeakers(): Flow<List<Speaker>> {
        return speakerDao.getConnectedSpeakers()
    }
    
    suspend fun insertSpeaker(speaker: Speaker) {
        speakerDao.insertSpeaker(speaker)
    }
    
    suspend fun updateSpeaker(speaker: Speaker) {
        speakerDao.updateSpeaker(speaker)
    }
    
    suspend fun deleteSpeaker(speaker: Speaker) {
        speakerDao.deleteSpeaker(speaker)
    }
    
    suspend fun deleteSpeakerById(id: String) {
        speakerDao.deleteSpeakerById(id)
    }
    
    suspend fun updateConnectionStatus(id: String, isConnected: Boolean) {
        speakerDao.updateConnectionStatus(id, isConnected)
    }
    
    suspend fun updateSpeakerPosition(
        id: String,
        height: Double,
        distanceFromWall: Double,
        distanceFromCorner: Double,
        angle: Double
    ) {
        speakerDao.updateSpeakerPosition(id, height, distanceFromWall, distanceFromCorner, angle)
    }
}

class RoomRepository(private val roomDao: RoomDao) {
    
    val allRooms: Flow<List<Room>> = roomDao.getAllRooms()
    
    suspend fun getRoomById(id: String): Room? {
        return roomDao.getRoomById(id)
    }
    
    suspend fun insertRoom(room: Room) {
        roomDao.insertRoom(room)
    }
    
    suspend fun updateRoom(room: Room) {
        roomDao.updateRoom(room)
    }
    
    suspend fun deleteRoom(room: Room) {
        roomDao.deleteRoom(room)
    }
    
    suspend fun deleteRoomById(id: String) {
        roomDao.deleteRoomById(id)
    }
}
