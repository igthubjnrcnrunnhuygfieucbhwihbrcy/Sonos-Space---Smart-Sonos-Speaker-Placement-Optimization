package com.sonosequencer.alternative.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.sonosequencer.alternative.R
import com.sonosequencer.alternative.SonosApp
import com.sonosequencer.alternative.data.FurnitureLevel
import com.sonosequencer.alternative.data.Room
import com.sonosequencer.alternative.data.RoomShape
import com.sonosequencer.alternative.data.WallMaterial
import com.sonosequencer.alternative.databinding.ActivityRoomSetupBinding
import kotlinx.coroutines.launch

class RoomSetupActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRoomSetupBinding
    private var selectedRoomShape: RoomShape = RoomShape.RECTANGULAR
    private var selectedWallMaterial: WallMaterial = WallMaterial.BRICK
    private var selectedFurnitureLevel: FurnitureLevel = FurnitureLevel.LIGHT
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (SonosApp.instance.isDarkModeEnabled()) {
            setTheme(R.style.Theme_SonosSequencer_Dark)
        } else {
            setTheme(R.style.Theme_SonosSequencer)
        }
        
        binding = ActivityRoomSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRoomShapeChips()
        setupWallMaterialChips()
        setupFurnitureLevelChips()
        setupSaveButton()
        
        // Check if we're editing an existing room
        val roomId = intent.getStringExtra("ROOM_ID")
        if (roomId != null) {
            loadExistingRoom(roomId)
        }
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
    
    private fun setupRoomShapeChips() {
        binding.cgRoomShape.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds[0])
                selectedRoomShape = when (chip.id) {
                    R.id.chipRectangular -> RoomShape.RECTANGULAR
                    R.id.chipSquare -> RoomShape.SQUARE
                    R.id.chipLShaped -> RoomShape.L_SHAPED
                    R.id.chipOpenPlan -> RoomShape.OPEN_PLAN
                    else -> RoomShape.RECTANGULAR
                }
            }
        }
    }
    
    private fun setupWallMaterialChips() {
        binding.cgWallMaterial.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds[0])
                selectedWallMaterial = when (chip.id) {
                    R.id.chipConcrete -> WallMaterial.CONCRETE
                    R.id.chipBrick -> WallMaterial.BRICK
                    R.id.chipDrywall -> WallMaterial.DRYWALL
                    R.id.chipWood -> WallMaterial.WOOD
                    R.id.chipGlass -> WallMaterial.GLASS
                    else -> WallMaterial.BRICK
                }
            }
        }
    }
    
    private fun setupFurnitureLevelChips() {
        binding.cgFurnitureLevel.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds[0])
                selectedFurnitureLevel = when (chip.id) {
                    R.id.chipMinimal -> FurnitureLevel.MINIMAL
                    R.id.chipLight -> FurnitureLevel.LIGHT
                    R.id.chipMedium -> FurnitureLevel.MEDIUM
                    R.id.chipHeavy -> FurnitureLevel.HEAVY
                    else -> FurnitureLevel.LIGHT
                }
            }
        }
    }
    
    private fun setupSaveButton() {
        binding.btnSaveRoom.setOnClickListener {
            saveRoom()
        }
        
        // Validate inputs
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        }
        
        binding.etRoomName.addTextChangedListener(textWatcher)
        binding.etRoomLength.addTextChangedListener(textWatcher)
        binding.etRoomWidth.addTextChangedListener(textWatcher)
        binding.etRoomHeight.addTextChangedListener(textWatcher)
    }
    
    private fun validateInputs(): Boolean {
        val name = binding.etRoomName.text.toString().trim()
        val length = binding.etRoomLength.text.toString().trim()
        val width = binding.etRoomWidth.text.toString().trim()
        val height = binding.etRoomHeight.text.toString().trim()
        
        val isValid = name.isNotEmpty() && 
                      length.isNotEmpty() && 
                      width.isNotEmpty() && 
                      height.isNotEmpty()
        
        binding.btnSaveRoom.isEnabled = isValid
        
        return isValid
    }
    
    private fun loadExistingRoom(roomId: String) {
        lifecycleScope.launch {
            val room = SonosApp.roomRepository.getRoomById(roomId)
            if (room != null) {
                binding.etRoomName.setText(room.name)
                binding.etRoomLength.setText(String.format("%.0f", room.lengthCm))
                binding.etRoomWidth.setText(String.format("%.0f", room.widthCm))
                binding.etRoomHeight.setText(String.format("%.0f", room.heightCm))
                
                // Select the correct chips
                when (room.shape) {
                    RoomShape.RECTANGULAR -> binding.chipRectangular.isChecked = true
                    RoomShape.SQUARE -> binding.chipSquare.isChecked = true
                    RoomShape.L_SHAPED -> binding.chipLShaped.isChecked = true
                    RoomShape.OPEN_PLAN -> binding.chipOpenPlan.isChecked = true
                }
                
                when (room.wallMaterial) {
                    WallMaterial.CONCRETE -> binding.chipConcrete.isChecked = true
                    WallMaterial.BRICK -> binding.chipBrick.isChecked = true
                    WallMaterial.DRYWALL -> binding.chipDrywall.isChecked = true
                    WallMaterial.WOOD -> binding.chipWood.isChecked = true
                    WallMaterial.GLASS -> binding.chipGlass.isChecked = true
                }
                
                when (room.furnitureLevel) {
                    FurnitureLevel.MINIMAL -> binding.chipMinimal.isChecked = true
                    FurnitureLevel.LIGHT -> binding.chipLight.isChecked = true
                    FurnitureLevel.MEDIUM -> binding.chipMedium.isChecked = true
                    FurnitureLevel.HEAVY -> binding.chipHeavy.isChecked = true
                }
            } else {
                Toast.makeText(this@RoomSetupActivity, "Room not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun saveRoom() {
        val name = binding.etRoomName.text.toString().trim()
        val length = binding.etRoomLength.text.toString().toDoubleOrNull() ?: 0.0
        val width = binding.etRoomWidth.text.toString().toDoubleOrNull() ?: 0.0
        val height = binding.etRoomHeight.text.toString().toDoubleOrNull() ?: 0.0
        
        if (!validateInputs()) {
            Snackbar.make(binding.root, "Please fill in all fields", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        val room = Room(
            name = name,
            lengthCm = length,
            widthCm = width,
            heightCm = height,
            shape = selectedRoomShape,
            wallMaterial = selectedWallMaterial,
            furnitureLevel = selectedFurnitureLevel
        )
        
        lifecycleScope.launch {
            try {
                SonosApp.roomRepository.insertRoom(room)
                
                Snackbar.make(binding.root, "Room saved", Snackbar.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error saving room: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
