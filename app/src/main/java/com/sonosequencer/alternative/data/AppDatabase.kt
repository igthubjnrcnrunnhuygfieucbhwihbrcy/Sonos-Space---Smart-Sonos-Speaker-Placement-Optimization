package com.sonosequencer.alternative.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Speaker::class, Room::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(SpeakerTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun speakerDao(): SpeakerDao
    abstract fun roomDao(): RoomDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sonos_space_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
