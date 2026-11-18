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
import kotlinx.coroutines.launch

class SpeakerSetupActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySpeakerSetupBinding
    private lateinit var sonosDiscovery: SonosDiscovery
    private var currentStep = 1
    private var currentAngle: Float = 0f
    private var isMeasuring = false
    private var discoveredSpeakers = listOf<com.sonosequencer.alternative.network.SonosDevice>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (SonosApp.instance.isDarkModeEnabled()) {
            setTheme(R.style.Theme_SonosSequencer_Dark)
        } else {
            setTheme(R.style.Theme_SonosSequencer)
        }
        
        binding = ActivitySpeakerSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sonosDiscovery = SonosDiscovery(this)
        
        setupToolbar()
        setupStepViews()
        setupStep1()
        setupStep2()
        setupStep3()
        
        // Check if we're editing an existing speaker
        val speakerId = intent.getStringExtra("SPEAKER_ID")
        if (speakerId != null) {
            loadExistingSpeaker(speakerId)
        } else {
            // New speaker setup
            showStep(1)
        }
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
    
    private fun setupStepViews() {
        // In a real app, you'd use a proper stepper view
        // For now, we'll just show/hide the step layouts
    }
    
    private fun setupStep1() {
        binding.btnSelectSpeaker.setOnClickListener {
            discoverSpeakers()
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
        
        // Validate inputs
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
    }
    
    private fun discoverSpeakers() {
        lifecycleScope.launch {
            binding.speakerList.visibility = View.GONE
            binding.btnSelectSpeaker.isEnabled = false
            
            try {
                discoveredSpeakers = sonosDiscovery.discoverDevices()
                
                if (discoveredSpeakers.isEmpty()) {
                    Snackbar.make(
                        binding.root,
                        R.string.no_speakers_found,
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    // Show speaker list
                    binding.speakerList.visibility = View.VISIBLE
                    // In a real app, you'd set up a RecyclerView with the discovered speakers
                }
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Error discovering speakers: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            } finally {
                binding.btnSelectSpeaker.isEnabled = true
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
        
        isMeasuring = true
        binding.btnMeasureAngle.text = getString(R.string.measuring_angle)
        
        lifecycleScope.launch {
            SonosApp.sensorManager.angleFlow.collect { angle ->
                currentAngle = angle
                binding.angleText.text = getString(R.string.angle_calculated, angle)
            }
        }
        
        SonosApp.sensorManager.startAngleMeasurement()
    }
    
    private fun stopAngleMeasurement() {
        SonosApp.sensorManager.stopAngleMeasurement()
        isMeasuring = false
        binding.btnMeasureAngle.text = getString(R.string.start_measurement)
        
        // Update the angle field in step 3
        binding.etAngle.setText(String.format("%.1f", currentAngle))
    }
    
    private fun showStep(step: Int) {
        currentStep = step
        
        binding.step1Layout.visibility = if (step == 1) View.VISIBLE else View.GONE
        binding.step2Layout.visibility = if (step == 2) View.VISIBLE else View.GONE
        binding.step3Layout.visibility = if (step == 3) View.VISIBLE else View.GONE
        
        // Update stepper view
        updateStepper()
    }
    
    private fun updateStepper() {
        // In a real app, update the stepper view
    }
    
    private fun loadExistingSpeaker(speakerId: String) {
        lifecycleScope.launch {
            val speaker = SonosApp.speakerRepository.getSpeakerById(speakerId)
            if (speaker != null) {
                // Populate the form with existing data
                binding.etSpeakerName.setText(speaker.name)
                binding.etHeight.setText(String.format("%.0f", speaker.heightCm))
                binding.etDistanceFromWall.setText(String.format("%.0f", speaker.distanceFromWallCm))
                binding.etDistanceFromCorner.setText(String.format("%.0f", speaker.distanceFromCornerCm))
                binding.etAngle.setText(String.format("%.0f", speaker.angleDegrees))
                
                showStep(3)
            } else {
                Toast.makeText(this@SpeakerSetupActivity, "Speaker not found", Toast.LENGTH_SHORT).show()
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
        val angle = binding.etAngle.text.toString().toDoubleOrNull() ?: 0.0
        
        if (!validateStep3Inputs()) {
            Snackbar.make(binding.root, "Please fill in all fields", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        // Create or update speaker
        val speakerId = intent.getStringExtra("SPEAKER_ID")
        val speaker = Speaker(
            id = speakerId ?: "",
            name = name,
            ipAddress = "192.168.1.100", // Placeholder
            model = "Unknown",
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
                    Snackbar.make(binding.root, "Speaker updated", Snackbar.LENGTH_SHORT).show()
                } else {
                    SonosApp.speakerRepository.insertSpeaker(speaker)
                    Snackbar.make(binding.root, "Speaker saved", Snackbar.LENGTH_SHORT).show()
                }
                
                // Clear the form and return to main screen
                finish()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error saving speaker: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isMeasuring) {
            SonosApp.sensorManager.stopAngleMeasurement()
        }
    }
}
