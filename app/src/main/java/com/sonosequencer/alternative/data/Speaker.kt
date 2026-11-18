package com.sonosequencer.alternative.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.UUID

@Entity(tableName = "speakers")
@TypeConverters(SpeakerTypeConverters::class)
data class Speaker (
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ipAddress: String,
    val model: String,
    val macAddress: String,
    val roomId: String? = null,
    
    // Position data
    val heightCm: Double,
    val distanceFromWallCm: Double,
    val distanceFromCornerCm: Double,
    val angleDegrees: Double,
    
    // Optimization settings
    val eqBass: Int = 50,
    val eqTreble: Int = 50,
    val loudness: Boolean = false,
    val balance: Int = 0,
    
    // Connection status
    val isConnected: Boolean = false,
    val signalStrength: Int = 0,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Room (
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val lengthCm: Double,
    val widthCm: Double,
    val heightCm: Double,
    val shape: RoomShape = RoomShape.RECTANGULAR,
    val wallMaterial: WallMaterial = WallMaterial.BRICK,
    val furnitureLevel: FurnitureLevel = FurnitureLevel.LIGHT,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class RoomShape {
    RECTANGULAR, SQUARE, L_SHAPED, OPEN_PLAN
}

enum class WallMaterial {
    CONCRETE, BRICK, DRYWALL, WOOD, GLASS
}

enum class FurnitureLevel {
    MINIMAL, LIGHT, MEDIUM, HEAVY
}

data class SpeakerPosition (
    val heightCm: Double,
    val distanceFromWallCm: Double,
    val distanceFromCornerCm: Double,
    val angleDegrees: Double
)

class SpeakerTypeConverters {
    @androidx.room.TypeConverter
    fun fromRoomShape(value: String): RoomShape {
        return RoomShape.valueOf(value)
    }
    
    @androidx.room.TypeConverter
    fun toRoomShape(shape: RoomShape): String {
        return shape.name
    }
    
    @androidx.room.TypeConverter
    fun fromWallMaterial(value: String): WallMaterial {
        return WallMaterial.valueOf(value)
    }
    
    @androidx.room.TypeConverter
    fun toWallMaterial(material: WallMaterial): String {
        return material.name
    }
    
    @androidx.room.TypeConverter
    fun fromFurnitureLevel(value: String): FurnitureLevel {
        return FurnitureLevel.valueOf(value)
    }
    
    @androidx.room.TypeConverter
    fun toFurnitureLevel(level: FurnitureLevel): String {
        return level.name
    }
}
