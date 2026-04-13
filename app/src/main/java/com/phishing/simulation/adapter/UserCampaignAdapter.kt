package com.phishing.simulation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.phishing.simulation.databinding.ItemUserCampaignBinding
import com.phishing.simulation.model.Campaign

class UserCampaignAdapter(
    private val onCampaignClick: (Campaign) -> Unit
) : ListAdapter<Campaign, UserCampaignAdapter.ViewHolder>(CampaignDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserCampaignBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onCampaignClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemUserCampaignBinding,
        private val onCampaignClick: (Campaign) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(campaign: Campaign) {
            binding.tvCampaignTitle.text = campaign.title
            binding.tvCampaignDescription.text = campaign.description
            
            binding.root.setOnClickListener {
                onCampaignClick(campaign)
            }
        }
    }

    private class CampaignDiffCallback : DiffUtil.ItemCallback<Campaign>() {
        override fun areItemsTheSame(oldItem: Campaign, newItem: Campaign): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Campaign, newItem: Campaign): Boolean {
            return oldItem == newItem
        }
    }
}
