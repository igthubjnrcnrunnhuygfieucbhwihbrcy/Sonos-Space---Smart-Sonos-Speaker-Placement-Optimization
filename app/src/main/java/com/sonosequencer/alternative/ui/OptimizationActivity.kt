package com.sonosequencer.alternative.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.sonosequencer.alternative.R
import com.sonosequencer.alternative.SonosApp
import com.sonosequencer.alternative.data.Speaker
import com.sonosequencer.alternative.utils.AcousticCalculator
import kotlinx.coroutines.launch

/**
 * Activity for optimizing speaker settings based on room acoustics.
 */
class OptimizationActivity : AppCompatActivity() {
    
    private lateinit var speakerRecyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var emptyState: View
    private lateinit var speakerAdapter: OptimizationAdapter
    private val acousticCalculator = AcousticCalculator()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (SonosApp.instance.isDarkModeEnabled()) {
            setTheme(R.style.Theme_SonosSequencer_Dark)
        } else {
            setTheme(R.style.Theme_SonosSequencer)
        }
        
        setContentView(R.layout.activity_optimization)
        
        // Find views
        speakerRecyclerView = findViewById(R.id.speakerRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        
        setupToolbar()
        setupRecyclerView()
        
        loadSpeakersAndOptimize()
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
    
    private fun setupRecyclerView() {
        speakerAdapter = OptimizationAdapter(
            onApplyClick = { speaker, eqSettings ->
                applySettings(speaker, eqSettings)
            }
        )
        
        speakerRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@OptimizationActivity)
            adapter = speakerAdapter
            setHasFixedSize(true)
        }
    }
    
    private fun loadSpeakersAndOptimize() {
        lifecycleScope.launch {
            SonosApp.speakerRepository.allSpeakers.collect { speakers ->
                if (speakers.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    speakerRecyclerView.visibility = View.GONE
                } else {
                    emptyState.visibility = View.GONE
                    speakerRecyclerView.visibility = View.VISIBLE
                    
                    // Calculate optimization for each speaker
                    val optimizedSpeakers = speakers.map { speaker ->
                        val eqSettings = acousticCalculator.calculateOptimalEQ(
                            speaker,
                            com.sonosequencer.alternative.data.Room(
                                name = "Default Room",
                                lengthCm = 500.0, // Default 5m
                                widthCm = 400.0,  // Default 4m
                                heightCm = 250.0  // Default 2.5m
                            )
                        )
                        Pair(speaker, eqSettings)
                    }
                    
                    speakerAdapter.submitList(optimizedSpeakers)
                }
            }
        }
    }
    
    private fun applySettings(speaker: Speaker, eqSettings: AcousticCalculator.EQSettings) {
        // In a real app, this would send the settings to the Sonos speaker
        // For now, just show a confirmation
        Snackbar.make(
            findViewById(android.R.id.content),
            "Settings applied to ${speaker.name}: Bass=${eqSettings.bass}, Treble=${eqSettings.treble}",
            Snackbar.LENGTH_LONG
        ).show()
    }
}

/**
 * Adapter for displaying speakers with their optimized settings.
 */
class OptimizationAdapter(
    private val onApplyClick: (Speaker, AcousticCalculator.EQSettings) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Pair<Speaker, AcousticCalculator.EQSettings>, 
        OptimizationAdapter.OptimizationViewHolder>(SpeakerDiffCallback) {
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): OptimizationViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_speaker_optimization, parent, false)
        return OptimizationViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: OptimizationViewHolder, position: Int) {
        val (speaker, eqSettings) = getItem(position)
        holder.bind(speaker, eqSettings)
    }
    
    inner class OptimizationViewHolder(view: View) : 
        androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        
        private val speakerName: android.widget.TextView = view.findViewById(R.id.speakerName)
        private val speakerModel: android.widget.TextView = view.findViewById(R.id.speakerModel)
        private val eqBass: android.widget.TextView = view.findViewById(R.id.eqBass)
        private val eqTreble: android.widget.TextView = view.findViewById(R.id.eqTreble)
        private val eqLoudness: android.widget.TextView = view.findViewById(R.id.eqLoudness)
        private val eqBalance: android.widget.TextView = view.findViewById(R.id.eqBalance)
        private val btnApply: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btnApply)
        
        fun bind(speaker: Speaker, eqSettings: AcousticCalculator.EQSettings) {
            speakerName.text = speaker.name
            speakerModel.text = speaker.model
            
            // Set EQ values
            eqBass.text = "Bass: ${eqSettings.bass}"
            eqTreble.text = "Treble: ${eqSettings.treble}"
            eqLoudness.text = "Loudness: ${if (eqSettings.loudness) "ON" else "OFF"}"
            eqBalance.text = "Balance: ${eqSettings.balance}"
            
            // Set click listener
            btnApply.setOnClickListener {
                onApplyClick(speaker, eqSettings)
            }
        }
    }
    
    object SpeakerDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<Pair<Speaker, AcousticCalculator.EQSettings>>() {
        override fun areItemsTheSame(oldItem: Pair<Speaker, AcousticCalculator.EQSettings>, newItem: Pair<Speaker, AcousticCalculator.EQSettings>): Boolean {
            return oldItem.first.id == newItem.first.id
        }
        
        override fun areContentsTheSame(oldItem: Pair<Speaker, AcousticCalculator.EQSettings>, newItem: Pair<Speaker, AcousticCalculator.EQSettings>): Boolean {
            return oldItem == newItem
        }
    }
}
