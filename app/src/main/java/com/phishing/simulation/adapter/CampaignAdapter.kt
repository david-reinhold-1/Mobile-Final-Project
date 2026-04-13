package com.phishing.simulation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.phishing.simulation.databinding.ItemCampaignBinding
import com.phishing.simulation.model.Campaign
import java.text.SimpleDateFormat
import java.util.Locale

class CampaignAdapter(
    private val onDeleteClick: (Campaign) -> Unit,
    private val onEditClick: (Campaign) -> Unit
) : ListAdapter<Campaign, CampaignAdapter.CampaignViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CampaignViewHolder {
        val binding = ItemCampaignBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CampaignViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CampaignViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CampaignViewHolder(
        private val binding: ItemCampaignBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(campaign: Campaign) {
            binding.tvCampaignTitle.text = campaign.title
            binding.tvCampaignBody.text = campaign.description
            binding.tvCampaignUrl.text = campaign.landingPageUrl
            binding.tvCreatedBy.text = "By: ${campaign.createdBy}"
            binding.tvDepartment.text = campaign.department.ifEmpty { "All" }
            binding.tvCreatedAt.text = dateFormat.format(campaign.createdAt.toDate())
            binding.btnDelete.setOnClickListener { onDeleteClick(campaign) }
            binding.btnEdit.setOnClickListener { onEditClick(campaign) }
        }
    }

    private companion object DiffCallback : DiffUtil.ItemCallback<Campaign>() {
        override fun areItemsTheSame(oldItem: Campaign, newItem: Campaign) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Campaign, newItem: Campaign) =
            oldItem == newItem
    }
}
