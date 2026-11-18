package com.sonosequencer.alternative

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.sonosequencer.alternative.data.AppDatabase
import com.sonosequencer.alternative.data.SpeakerRepository
import com.sonosequencer.alternative.network.SonosApiService
import com.sonosequencer.alternative.utils.SensorManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class SonosApp : Application() {
    
    companion object {
        private const val PREF_NAME = "SonosSpacePrefs"
        private const val PREF_VERSION_CODE = "version_code"
        private const val PREF_DARK_MODE = "dark_mode"
        private const val PREF_AUTO_UPDATE = "auto_update"
        private const val PREF_UNITS = "units"
        
        lateinit var instance: SonosApp
            private set
        
        val database: AppDatabase by lazy { AppDatabase.getDatabase(instance) }
        val speakerRepository: SpeakerRepository by lazy { SpeakerRepository(database.speakerDao()) }
        val roomRepository: RoomRepository by lazy { RoomRepository(database.roomDao()) }
        val sonosApi: SonosApiService by lazy { createSonosApiService() }
        val sensorManager: SensorManager by lazy { SensorManager(instance) }
        
        private fun createSonosApiService(): SonosApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
            
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.sonos.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            return retrofit.create(SonosApiService::class.java)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize preferences
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getInt(PREF_VERSION_CODE, 0) == 0) {
            // First run
            prefs.edit()
                .putInt(PREF_VERSION_CODE, 1)
                .putBoolean(PREF_DARK_MODE, false)
                .putBoolean(PREF_AUTO_UPDATE, true)
                .putString(PREF_UNITS, "cm")
                .apply()
        }
    }
    
    fun getPreferences(): SharedPreferences {
        return getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    fun isDarkModeEnabled(): Boolean {
        return getPreferences().getBoolean(PREF_DARK_MODE, false)
    }
    
    fun isAutoUpdateEnabled(): Boolean {
        return getPreferences().getBoolean(PREF_AUTO_UPDATE, true)
    }
    
    fun getUnits(): String {
        return getPreferences().getString(PREF_UNITS, "cm") ?: "cm"
    }
    
    fun setDarkMode(enabled: Boolean) {
        getPreferences().edit().putBoolean(PREF_DARK_MODE, enabled).apply()
    }
    
    fun setAutoUpdate(enabled: Boolean) {
        getPreferences().edit().putBoolean(PREF_AUTO_UPDATE, enabled).apply()
    }
    
    fun setUnits(units: String) {
        getPreferences().edit().putString(PREF_UNITS, units).apply()
    }
}
