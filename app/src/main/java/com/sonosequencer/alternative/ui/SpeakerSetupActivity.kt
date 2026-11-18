package com.sonosequencer.alternative.ui

import android.hardware.Sensor
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.sonosequencer.alternative.R
import com.sonosequencer.alternative.SonosApp
import com.sonosequencer.alternative.data.Speaker
import com.sonosequencer.alternative.databinding.ActivitySpeakerSetupBinding
import com.sonosequencer.alternative.network.SonosDiscovery
import com.sonosequencer.alternative.network.SonosDevice
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Activity for setting up a new speaker or editing an existing one.
 * 
 * This activity guides the user through a 3-step process:
 * 1. Discover and select a Sonos speaker
 * 2. Measure the angle by placing phone on speaker
 * 3. Enter position details and save
 */
class SpeakerSetupActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySpeakerSetupBinding
    private lateinit var sonosDiscovery: SonosDiscovery
    
    private var currentStep = 1
    private var currentAngle: Float = 0f
    private var isMeasuring = false
    private var discoveredSpeakers = listOf<SonosDevice>()
    private var selectedSpeaker: SonosDevice? = null
    
    // For collecting angle updates
    private var angleCollectionJob: Job? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set theme based on preferences
        if (SonosApp.instance.isDarkModeEnabled()) {
            setTheme(R.style.Theme_SonosSequencer_Dark)
        } else {
            setTheme(R.style.Theme_SonosSequencer)
        }
        
        binding = ActivitySpeakerSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sonosDiscovery = SonosDiscovery(this)
        
        setupToolbar()
        setupStep1()
        setupStep2()
        setupStep3()
        
        // Check if we're editing an existing speaker
        val speakerId = intent.getStringExtra("SPEAKER_ID")
        if (speakerId != null) {
            loadExistingSpeaker(speakerId)
        } else {
            // New speaker setup - start with step 1
            showStep(1)
        }
        
        // Update stepper view
        updateStepperView()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
    
    private fun setupStep1() {
        binding.btnSelectSpeaker.setOnClickListener {
            if (sonosDiscovery.isWifiConnected()) {
                discoverSpeakers()
            } else {
                Snackbar.make(
                    binding.root,
                    "Please connect to WiFi to discover speakers",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun setupStep2() {
        binding.btnMeasureAngle.setOnClickListener {
            startAngleMeasurement()
        }
        
        binding.btnNextStep2.setOnClickListener {
            stopAngleMeasurement()
            showStep(3)
        }
    }
    
    private fun setupStep3() {
        binding.btnSaveSpeaker.setOnClickListener {
            saveSpeaker()
        }
        
        // Validate inputs in real-time
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateStep3Inputs()
            }
        }
        
        binding.etHeight.addTextChangedListener(textWatcher)
        binding.etDistanceFromWall.addTextChangedListener(textWatcher)
        binding.etDistanceFromCorner.addTextChangedListener(textWatcher)
        binding.etSpeakerName.addTextChangedListener(textWatcher)
        
        // Set initial button state
        validateStep3Inputs()
    }
    
    private fun discoverSpeakers() {
        lifecycleScope.launch {
            binding.speakerList.visibility = View.GONE
            binding.btnSelectSpeaker.isEnabled = false
            binding.btnSelectSpeaker.text = getString(R.string.searching_speakers)
            
            try {
                discoveredSpeakers = sonosDiscovery.discoverDevices()
                
                if (discoveredSpeakers.isEmpty()) {
                    Snackbar.make(
                        binding.root,
                        R.string.no_speakers_found,
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    // For now, just select the first speaker automatically
                    // In a real app, you'd show a list and let user select
                    selectedSpeaker = discoveredSpeakers.firstOrNull()
                    if (selectedSpeaker != null) {
                        Snackbar.make(
                            binding.root,
                            "Found ${discoveredSpeakers.size} speaker(s). Using: ${selectedSpeaker?.friendlyName}",
                            Snackbar.LENGTH_LONG
                        ).show()
                        
                        // Pre-fill speaker name if available
                        selectedSpeaker?.friendlyName?.let { name ->
                            binding.etSpeakerName.setText(name)
                        }
                        
                        // Move to step 2
                        showStep(2)
                    }
                }
            } catch (e: SecurityException) {
                Snackbar.make(
                    binding.root,
                    "Permission denied. Please enable location/WiFi permissions.",
                    Snackbar.LENGTH_LONG
                ).show()
                Log.e("SpeakerSetup", "Permission error", e)
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Error discovering speakers: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
                Log.e("SpeakerSetup", "Discovery error", e)
            } finally {
                binding.btnSelectSpeaker.isEnabled = true
                binding.btnSelectSpeaker.text = getString(R.string.discover_speakers)
            }
        }
    }
    
    private fun startAngleMeasurement() {
        if (!SonosApp.sensorManager.hasRequiredSensors()) {
            Snackbar.make(
                binding.root,
                R.string.error_sensors,
                Snackbar.LENGTH_LONG
            ).show()
            return
        }
        
        // Stop any existing measurement
        stopAngleMeasurement()
        
        // Start new measurement
        val success = SonosApp.sensorManager.startAngleMeasurement()
        if (!success) {
            Snackbar.make(
                binding.root,
                "Failed to start sensor measurement",
                Snackbar.LENGTH_LONG
            ).show()
            return
        }
        
        isMeasuring = true
        binding.btnMeasureAngle.text = getString(R.string.measuring_angle)
        binding.angleText.text = getString(R.string.measuring_angle)
        
        // Collect angle updates
        angleCollectionJob = SonosApp.sensorManager.angleFlow
            .catch { e ->
                Log.e("SpeakerSetup", "Error in angle flow", e)
                lifecycleScope.launch {
                    Snackbar.make(
                        binding.root,
                        "Sensor error: ${e.message}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            .onEach { angle ->
                currentAngle = angle
                runOnUiThread {
                    binding.angleText.text = getString(R.string.angle_calculated, angle)
                }
            }
            .launchIn(lifecycleScope)
    }
    
    private fun stopAngleMeasurement() {
        // Cancel angle collection
        angleCollectionJob?.cancel()
        angleCollectionJob = null
        
        // Stop sensor measurement
        SonosApp.sensorManager.stopAngleMeasurement()
        isMeasuring = false
        
        binding.btnMeasureAngle.text = getString(R.string.start_measurement)
    }
    
    private fun showStep(step: Int) {
        currentStep = step
        
        binding.step1Layout.visibility = if (step == 1) View.VISIBLE else View.GONE
        binding.step2Layout.visibility = if (step == 2) View.VISIBLE else View.GONE
        binding.step3Layout.visibility = if (step == 3) View.VISIBLE else View.GONE
        
        updateStepperView()
    }
    
    private fun updateStepperView() {
        // Simple stepper indication
        when (currentStep) {
            1 -> {
                binding.toolbar.title = getString(R.string.speaker_setup_title)
                binding.toolbar.subtitle = getString(R.string.discover_speakers)
            }
            2 -> {
                binding.toolbar.title = getString(R.string.speaker_setup_title)
                binding.toolbar.subtitle = getString(R.string.place_phone_on_speaker)
            }
            3 -> {
                binding.toolbar.title = getString(R.string.speaker_setup_title)
                binding.toolbar.subtitle = getString(R.string.speaker_position)
            }
        }
    }
    
    private fun loadExistingSpeaker(speakerId: String) {
        lifecycleScope.launch {
            try {
                val speaker = SonosApp.speakerRepository.getSpeakerById(speakerId)
                if (speaker != null) {
                    // Populate the form with existing data
                    binding.etSpeakerName.setText(speaker.name)
                    binding.etHeight.setText(String.format("%.0f", speaker.heightCm))
                    binding.etDistanceFromWall.setText(String.format("%.0f", speaker.distanceFromWallCm))
                    binding.etDistanceFromCorner.setText(String.format("%.0f", speaker.distanceFromCornerCm))
                    binding.etAngle.setText(String.format("%.0f", speaker.angleDegrees))
                    
                    currentAngle = speaker.angleDegrees.toFloat()
                    
                    // Start at step 3 for editing
                    showStep(3)
                } else {
                    Toast.makeText(this@SpeakerSetupActivity, "Speaker not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SpeakerSetupActivity, "Error loading speaker: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun validateStep3Inputs(): Boolean {
        val name = binding.etSpeakerName.text.toString().trim()
        val height = binding.etHeight.text.toString().trim()
        val distanceFromWall = binding.etDistanceFromWall.text.toString().trim()
        val distanceFromCorner = binding.etDistanceFromCorner.text.toString().trim()
        
        val isValid = name.isNotEmpty() && 
                      height.isNotEmpty() && 
                      distanceFromWall.isNotEmpty() && 
                      distanceFromCorner.isNotEmpty()
        
        binding.btnSaveSpeaker.isEnabled = isValid
        
        return isValid
    }
    
    private fun saveSpeaker() {
        val name = binding.etSpeakerName.text.toString().trim()
        val height = binding.etHeight.text.toString().toDoubleOrNull() ?: 0.0
        val distanceFromWall = binding.etDistanceFromWall.text.toString().toDoubleOrNull() ?: 0.0
        val distanceFromCorner = binding.etDistanceFromCorner.text.toString().toDoubleOrNull() ?: 0.0
        val angle = binding.etAngle.text.toString().toDoubleOrNull() ?: currentAngle.toDouble()
        
        if (!validateStep3Inputs()) {
            Snackbar.make(binding.root, "Please fill in all fields", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        // Create or update speaker
        val speakerId = intent.getStringExtra("SPEAKER_ID")
        val speaker = Speaker(
            id = speakerId ?: "",
            name = name,
            ipAddress = selectedSpeaker?.ipAddress ?: "192.168.1.100",
            model = selectedSpeaker?.modelName ?: "Unknown",
            macAddress = "",
            heightCm = height,
            distanceFromWallCm = distanceFromWall,
            distanceFromCornerCm = distanceFromCorner,
            angleDegrees = angle
        )
        
        lifecycleScope.launch {
            try {
                if (speakerId != null) {
                    SonosApp.speakerRepository.updateSpeaker(speaker)
                    Snackbar.make(binding.root, "Speaker updated successfully!", Snackbar.LENGTH_SHORT).show()
                } else {
                    SonosApp.speakerRepository.insertSpeaker(speaker)
                    Snackbar.make(binding.root, "Speaker saved successfully!", Snackbar.LENGTH_SHORT).show()
                }
                
                // Clear the form and return to main screen
                finish()
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Error saving speaker: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
                Log.e("SpeakerSetup", "Error saving speaker", e)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Make sure to stop measurement when activity is destroyed
        stopAngleMeasurement()
    }
    
    override fun onPause() {
        super.onPause()
        // Stop measurement when activity is paused
        if (isMeasuring) {
            stopAngleMeasurement()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Re-start measurement if we were measuring before
        if (isMeasuring && currentStep == 2) {
            startAngleMeasurement()
        }
    }
}
