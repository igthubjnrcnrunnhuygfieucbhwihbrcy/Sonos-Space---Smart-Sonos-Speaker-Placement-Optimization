package com.sonosequencer.alternative.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.Snackbar
import com.sonosequencer.alternative.R
import com.sonosequencer.alternative.SonosApp
import com.sonosequencer.alternative.databinding.ActivitySettingsBinding
import com.sonosequencer.alternative.utils.UpdateManager
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (SonosApp.instance.isDarkModeEnabled()) {
            setTheme(R.style.Theme_SonosSequencer_Dark)
        } else {
            setTheme(R.style.Theme_SonosSequencer)
        }
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }
    
    class SettingsFragment : PreferenceFragmentCompat() {
        
        private lateinit var updateManager: UpdateManager
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            updateManager = UpdateManager(requireContext())
            
            setupPreferences()
        }
        
        private fun setupPreferences() {
            // Dark mode preference
            val darkModePref = findPreference<SwitchPreferenceCompat>("dark_mode")
            darkModePref?.isChecked = SonosApp.instance.isDarkModeEnabled()
            darkModePref?.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                SonosApp.instance.setDarkMode(enabled)
                
                // Restart activity to apply theme
                requireActivity().recreate()
                true
            }
            
            // Auto update preference
            val autoUpdatePref = findPreference<SwitchPreferenceCompat>("auto_update")
            autoUpdatePref?.isChecked = SonosApp.instance.isAutoUpdateEnabled()
            autoUpdatePref?.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                SonosApp.instance.setAutoUpdate(enabled)
                true
            }
            
            // Check for updates preference
            val checkUpdatesPref = findPreference<Preference>("check_for_updates")
            checkUpdatesPref?.setOnPreferenceClickListener {
                checkForUpdates()
                true
            }
            
            // Version preference
            val versionPref = findPreference<Preference>("app_version")
            versionPref?.summary = "v${BuildConfig.VERSION_NAME}"
            
            // Reset all preference
            val resetPref = findPreference<Preference>("reset_all")
            resetPref?.setOnPreferenceClickListener {
                showResetDialog()
                true
            }
        }
        
        private fun checkForUpdates() {
            lifecycleScope.launch {
                try {
                    val updateInfo = updateManager.checkForUpdates()
                    
                    if (updateInfo.isAvailable) {
                        updateManager.showUpdateDialog(
                            requireActivity(),
                            updateInfo,
                            onUpdate = {
                                updateManager.startUpdateFlow(
                                    requireActivity(),
                                    updateInfo.updateType
                                )
                            },
                            onCancel = {}
                        )
                    } else {
                        Snackbar.make(
                            requireView(),
                            "No updates available",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Snackbar.make(
                        requireView(),
                        "Error checking for updates: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
        
        private fun showResetDialog() {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.reset_all)
                .setMessage(R.string.reset_confirm)
                .setPositiveButton(R.string.ok) { _, _ ->
                    resetAllSettings()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        
        private fun resetAllSettings() {
            lifecycleScope.launch {
                try {
                    // Clear all speakers
                    SonosApp.speakerRepository.deleteAllSpeakers()
                    
                    // Reset preferences
                    SonosApp.instance.getPreferences().edit().clear().apply()
                    
                    // Set default values
                    SonosApp.instance.setDarkMode(false)
                    SonosApp.instance.setAutoUpdate(true)
                    
                    Toast.makeText(
                        requireContext(),
                        "All settings reset",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Restart activity
                    requireActivity().recreate()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Error resetting settings: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
