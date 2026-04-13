package com.phishing.simulation.adapter

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.phishing.simulation.databinding.ItemDetectionBinding
import com.phishing.simulation.model.Detection

class DetectionAdapter : ListAdapter<DetectionItem, DetectionAdapter.ViewHolder>(DetectionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemDetectionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DetectionItem) {
            binding.tvCampaignTitle.text = item.campaignTitle
            binding.tvUserEmail.text = item.userEmail
            
            val timestamp = item.detection.timestamp.toDate().time
            val now = System.currentTimeMillis()
            val relativeTime = DateUtils.getRelativeTimeSpanString(
                timestamp,
                now,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )
            binding.tvTimestamp.text = relativeTime
            
            val hasLocation = item.detection.location.latitude != 0.0 || 
                             item.detection.location.longitude != 0.0
            binding.tvLocation.text = if (hasLocation) "📍" else "❓"
        }
    }

    private class DetectionDiffCallback : DiffUtil.ItemCallback<DetectionItem>() {
        override fun areItemsTheSame(oldItem: DetectionItem, newItem: DetectionItem): Boolean {
            return oldItem.detection.timestamp == newItem.detection.timestamp &&
                   oldItem.detection.userId == newItem.detection.userId
        }

        override fun areContentsTheSame(oldItem: DetectionItem, newItem: DetectionItem): Boolean {
            return oldItem == newItem
        }
    }
}

data class DetectionItem(
    val detection: Detection,
    val campaignTitle: String,
    val userEmail: String
)
