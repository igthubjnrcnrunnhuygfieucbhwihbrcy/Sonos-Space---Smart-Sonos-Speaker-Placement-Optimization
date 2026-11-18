package com.sonosequencer.alternative.utils

import com.sonosequencer.alternative.data.FurnitureLevel
import com.sonosequencer.alternative.data.RoomShape
import com.sonosequencer.alternative.data.Speaker
import com.sonosequencer.alternative.data.WallMaterial
import kotlin.math.sqrt

// Calculator for room acoustics and speaker optimization
class AcousticCalculator {
    
    companion object {
        // Speed of sound in air at 20°C (m/s)
        private const val SPEED_OF_SOUND = 343.0
        
        // Reference distance for sound pressure level calculations (m)
        private const val REFERENCE_DISTANCE = 1.0
    }
    
    // Calculate optimal EQ settings based on room and speaker position
    fun calculateOptimalEQ(
        speaker: Speaker,
        room: com.sonosequencer.alternative.data.Room
    ): EQSettings {
        // Calculate room volume
        val roomVolume = room.lengthCm * room.widthCm * room.heightCm / 1_000_000.0 // m³
        
        // Calculate speaker distance from walls
        val distanceFromFrontWall = room.lengthCm - speaker.distanceFromWallCm
        val distanceFromSideWall = room.widthCm - speaker.distanceFromCornerCm
        
        // Calculate room modes (standing waves)
        val roomModes = calculateRoomModes(room)
        
        // Calculate bass boost based on room size and speaker position
        val bassBoost = calculateBassBoost(roomVolume, speaker, room)
        
        // Calculate treble adjustment based on wall material and furniture
        val trebleAdjustment = calculateTrebleAdjustment(room.wallMaterial, room.furnitureLevel)
        
        // Calculate loudness compensation
        val loudness = roomVolume > 50.0 // Enable loudness for larger rooms
        
        return EQSettings(
            bass = (50 + bassBoost).coerceIn(0, 100),
            treble = (50 + trebleAdjustment).coerceIn(0, 100),
            loudness = loudness,
            balance = calculateBalance(speaker, room)
        )
    }
    
    private fun calculateRoomModes(room: com.sonosequencer.alternative.data.Room): List<Double> {
        val modes = mutableListOf<Double>()
        
        // Calculate axial modes (simplified)
        val length = room.lengthCm / 100.0 // Convert to meters
        val width = room.widthCm / 100.0
        val height = room.heightCm / 100.0
        
        // Calculate first few modes for each dimension
        for (n in 0..2) {
            val modeX = (n * SPEED_OF_SOUND) / (2 * length)
            val modeY = (n * SPEED_OF_SOUND) / (2 * width)
            val modeZ = (n * SPEED_OF_SOUND) / (2 * height)
            
            if (modeX > 20) modes.add(modeX)
            if (modeY > 20) modes.add(modeY)
            if (modeZ > 20) modes.add(modeZ)
        }
        
        return modes
    }
    
    private fun calculateBassBoost(
        roomVolume: Double,
        speaker: Speaker,
        room: com.sonosequencer.alternative.data.Room
    ): Int {
        // Smaller rooms need more bass boost due to room gain
        // Rooms with more furniture absorb more bass
        
        var boost = 0
        
        // Room size factor
        when {
            roomVolume < 20 -> boost += 15 // Small room
            roomVolume < 50 -> boost += 10 // Medium room
            roomVolume < 100 -> boost += 5 // Large room
            else -> boost += 0 // Very large room
        }
        
        // Speaker position factor
        // Speakers in corners get more bass reinforcement
        if (speaker.distanceFromWallCm < 50 && speaker.distanceFromCornerCm < 50) {
            boost += 10 // Corner placement
        } else if (speaker.distanceFromWallCm < 100) {
            boost += 5 // Near wall
        }
        
        // Wall material factor
        when (room.wallMaterial) {
            WallMaterial.CONCRETE -> boost += 5 // Reflective
            WallMaterial.BRICK -> boost += 3
            WallMaterial.DRYWALL -> boost += 0
            WallMaterial.WOOD -> boost -= 2 // Absorptive
            WallMaterial.GLASS -> boost += 7 // Very reflective
        }
        
        // Furniture factor
        when (room.furnitureLevel) {
            FurnitureLevel.MINIMAL -> boost += 3
            FurnitureLevel.LIGHT -> boost += 0
            FurnitureLevel.MEDIUM -> boost -= 3
            FurnitureLevel.HEAVY -> boost -= 5
        }
        
        return boost.coerceIn(-20, 20)
    }
    
    private fun calculateTrebleAdjustment(
        wallMaterial: WallMaterial,
        furnitureLevel: FurnitureLevel
    ): Int {
        var adjustment = 0
        
        // Wall material factor
        when (wallMaterial) {
            WallMaterial.CONCRETE -> adjustment += 5 // Reflective, need more treble absorption
            WallMaterial.BRICK -> adjustment += 3
            WallMaterial.DRYWALL -> adjustment += 0
            WallMaterial.WOOD -> adjustment -= 2 // Absorptive
            WallMaterial.GLASS -> adjustment += 7 // Very reflective
        }
        
        // Furniture factor
        when (furnitureLevel) {
            FurnitureLevel.MINIMAL -> adjustment += 5
            FurnitureLevel.LIGHT -> adjustment += 2
            FurnitureLevel.MEDIUM -> adjustment += 0
            FurnitureLevel.HEAVY -> adjustment -= 3
        }
        
        return adjustment.coerceIn(-10, 10)
    }
    
    private fun calculateBalance(
        speaker: Speaker,
        room: com.sonosequencer.alternative.data.Room
    ): Int {
        // Calculate balance based on speaker position in room
        // Center of room
        val roomCenterX = room.widthCm / 2
        val roomCenterY = room.lengthCm / 2
        
        // Speaker position relative to room center
        val offsetX = speaker.distanceFromCornerCm - roomCenterX
        val offsetY = speaker.distanceFromWallCm - roomCenterY
        
        // Calculate balance (-100 to 100)
        val balanceX = (offsetX / roomCenterX * 100).toInt()
        val balanceY = (offsetY / roomCenterY * 100).toInt()
        
        // Combine for stereo balance
        return (balanceX.coerceIn(-100, 100) + balanceY.coerceIn(-100, 100)) / 2
    }
    
    // Calculate sound pressure level at a given distance
    fun calculateSPL(
        speaker: Speaker,
        distanceCm: Double,
        volume: Int
    ): Double {
        // Simplified SPL calculation
        // SPL decreases by 6 dB for each doubling of distance
        
        val distanceM = distanceCm / 100.0
        val referenceSPL = 80.0 + (volume / 2.0) // Reference SPL at 1m
        
        // Inverse square law: SPL = referenceSPL - 20 * log10(distance / referenceDistance)
        val spl = referenceSPL - 20 * Math.log10(distanceM / REFERENCE_DISTANCE)
        
        return spl.coerceAtLeast(0.0)
    }
    
    // Calculate optimal listening position
    fun calculateOptimalListeningPosition(
        speakers: List<Speaker>,
        room: com.sonosequencer.alternative.data.Room
    ): ListeningPosition {
        if (speakers.isEmpty()) {
            return ListeningPosition(
                xCm = room.widthCm / 2,
                yCm = room.lengthCm / 2,
                distanceCm = 0.0,
                quality = 0
            )
        }
        
        // For stereo setup, optimal position is equidistant from both speakers
        if (speakers.size >= 2) {
            val speaker1 = speakers[0]
            val speaker2 = speakers[1]
            
            // Calculate midpoint between speakers
            val midX = (speaker1.distanceFromCornerCm + speaker2.distanceFromCornerCm) / 2
            val midY = (speaker1.distanceFromWallCm + speaker2.distanceFromWallCm) / 2
            
            // Calculate distance between speakers
            val distanceBetween = sqrt(
                Math.pow((speaker2.distanceFromCornerCm - speaker1.distanceFromCornerCm).toDouble(), 2.0) +
                Math.pow((speaker2.distanceFromWallCm - speaker1.distanceFromWallCm).toDouble(), 2.0)
            )
            
            // Optimal distance from each speaker (forms equilateral triangle)
            val optimalDistance = distanceBetween * 0.866 // sqrt(3)/2
            
            return ListeningPosition(
                xCm = midX,
                yCm = midY,
                distanceCm = optimalDistance,
                quality = 90
            )
        }
        
        // For single speaker, optimal position is directly in front
        val speaker = speakers[0]
        return ListeningPosition(
            xCm = speaker.distanceFromCornerCm,
            yCm = room.lengthCm - 100, // 1m from front wall
            distanceCm = 100.0, // 1m from speaker
            quality = 70
        )
    }
    
    data class EQSettings(
        val bass: Int,
        val treble: Int,
        val loudness: Boolean,
        val balance: Int
    )
    
    data class ListeningPosition(
        val xCm: Double,
        val yCm: Double,
        val distanceCm: Double,
        val quality: Int // 0-100
    )
}
