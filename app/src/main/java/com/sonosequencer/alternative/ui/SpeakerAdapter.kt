package com.sonosequencer.alternative.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sonosequencer.alternative.R
import com.sonosequencer.alternative.data.Speaker
import com.sonosequencer.alternative.databinding.ItemSpeakerBinding

class SpeakerAdapter(
    private val onEditClick: (Speaker) -> Unit,
    private val onOptimizeClick: (Speaker) -> Unit
) : ListAdapter<Speaker, SpeakerAdapter.SpeakerViewHolder>(SpeakerDiffCallback) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeakerViewHolder {
        val binding = ItemSpeakerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SpeakerViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: SpeakerViewHolder, position: Int) {
        val speaker = getItem(position)
        holder.bind(speaker)
    }
    
    inner class SpeakerViewHolder(private val binding: ItemSpeakerBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(speaker: Speaker) {
            binding.speakerName.text = speaker.name
            binding.speakerModel.text = speaker.model
            binding.speakerStatus.text = if (speaker.isConnected) {
                binding.root.context.getString(R.string.connected)
            } else {
                binding.root.context.getString(R.string.disconnected)
            }
            
            // Set status color
            val statusColor = if (speaker.isConnected) {
                android.R.color.holo_green_light
            } else {
                android.R.color.holo_red_light
            }
            binding.speakerStatus.setTextColor(
                binding.root.context.resources.getColor(statusColor, null)
            )
            
            // Format position info
            val positionText = String.format(
                "Height: %.0fcm, Angle: %.0f°",
                speaker.heightCm,
                speaker.angleDegrees
            )
            binding.speakerPosition.text = positionText
            
            // Set click listeners
            binding.btnEdit.setOnClickListener {
                onEditClick(speaker)
            }
            
            binding.btnOptimize.setOnClickListener {
                onOptimizeClick(speaker)
            }
            
            // Set speaker icon based on model
            // In a real app, you'd have different icons for different models
        }
    }
    
    object SpeakerDiffCallback : DiffUtil.ItemCallback<Speaker>() {
        override fun areItemsTheSame(oldItem: Speaker, newItem: Speaker): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Speaker, newItem: Speaker): Boolean {
            return oldItem == newItem
        }
    }
}
