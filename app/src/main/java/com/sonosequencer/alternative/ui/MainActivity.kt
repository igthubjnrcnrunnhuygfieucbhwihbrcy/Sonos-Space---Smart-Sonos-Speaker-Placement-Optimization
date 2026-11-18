package com.sonosequencer.alternative.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.sonosequencer.alternative.R
import com.sonosequencer.alternative.SonosApp
import com.sonosequencer.alternative.data.Speaker
import com.sonosequencer.alternative.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var speakerAdapter: SpeakerAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set theme based on preferences
        if (SonosApp.instance.isDarkModeEnabled()) {
            setTheme(R.style.Theme_SonosSequencer_Dark)
        } else {
            setTheme(R.style.Theme_SonosSequencer)
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupBottomNavigation()
        setupFab()
        
        loadSpeakers()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            onOptionsItemSelected(menuItem)
        }
    }
    
    private fun setupRecyclerView() {
        speakerAdapter = SpeakerAdapter(
            onEditClick = { speaker ->
                openSpeakerSetup(speaker)
            },
            onOptimizeClick = { speaker ->
                optimizeSpeaker(speaker)
            }
        )
        
        binding.speakerRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = speakerAdapter
            setHasFixedSize(true)
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_speakers -> {
                    // Already on speakers tab
                    true
                }
                R.id.nav_setup -> {
                    startActivity(Intent(this, SpeakerSetupActivity::class.java))
                    true
                }
                R.id.nav_optimize -> {
                    // Open optimization screen
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupFab() {
        binding.fab.setOnClickListener {
            startActivity(Intent(this, SpeakerSetupActivity::class.java))
        }
    }
    
    private fun loadSpeakers() {
        lifecycleScope.launch {
            SonosApp.speakerRepository.allSpeakers.collect { speakers ->
                updateSpeakerList(speakers)
            }
        }
    }
    
    private fun updateSpeakerList(speakers: List<Speaker>) {
        if (speakers.isEmpty()) {
            binding.speakerRecyclerView.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.speakerRecyclerView.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            speakerAdapter.submitList(speakers)
        }
    }
    
    private fun openSpeakerSetup(speaker: Speaker? = null) {
        val intent = Intent(this, SpeakerSetupActivity::class.java).apply {
            speaker?.let { putExtra("SPEAKER_ID", it.id) }
        }
        startActivity(intent)
    }
    
    private fun optimizeSpeaker(speaker: Speaker) {
        // Open optimization screen for this speaker
        Snackbar.make(binding.root, "Optimizing ${speaker.name}", Snackbar.LENGTH_SHORT).show()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> {
                // Discover new speakers
                discoverSpeakers()
                true
            }
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.menu_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun discoverSpeakers() {
        Snackbar.make(binding.root, "Searching for Sonos speakers...", Snackbar.LENGTH_LONG).show()
        
        // In a real app, this would use SonosDiscovery
        // For now, just show a message
    }
    
    private fun showAboutDialog() {
        // Show about dialog
    }
    
    override fun onResume() {
        super.onResume()
        loadSpeakers()
    }
}
