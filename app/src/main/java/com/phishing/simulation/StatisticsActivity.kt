package com.phishing.simulation

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.phishing.simulation.adapter.DetectionAdapter
import com.phishing.simulation.adapter.DetectionItem
import com.phishing.simulation.databinding.ActivityStatisticsBinding
import com.phishing.simulation.model.Campaign
import com.phishing.simulation.model.Detection
import com.phishing.simulation.model.User
import com.phishing.simulation.repository.FirebaseRepository
import com.phishing.simulation.repository.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private val repository = FirebaseRepository()
    private lateinit var detectionAdapter: DetectionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadStatistics()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        
        val btnBack = binding.toolbar.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBack)
        btnBack?.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        detectionAdapter = DetectionAdapter()
        binding.rvDetections.layoutManager = LinearLayoutManager(this)
        binding.rvDetections.adapter = detectionAdapter
    }

    private fun loadStatistics() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val campaignsDeferred = async { repository.getCampaigns(limit = 100) }
                val detectionsDeferred = async { repository.getAllDetections() }

                val campaignsResult = campaignsDeferred.await()
                val detectionsResult = detectionsDeferred.await()

                if (campaignsResult is Result.Success && detectionsResult is Result.Success) {
                    val campaigns = campaignsResult.data.items
                    val detections = detectionsResult.data

                    if (campaigns.isEmpty() && detections.isEmpty()) {
                        showNoData()
                    } else {
                        displayStatistics(campaigns, detections)
                        displayChart(campaigns, detections)
                        displayDetections(campaigns, detections)
                    }
                } else {
                    handleError(campaignsResult, detectionsResult)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@StatisticsActivity,
                    "Error loading statistics: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                showNoData()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun displayStatistics(campaigns: List<Campaign>, detections: List<Detection>) {
        binding.layoutContent.visibility = View.VISIBLE
        binding.tvNoData.visibility = View.GONE

        binding.tvTotalCampaigns.text = campaigns.size.toString()
        binding.tvTotalDetections.text = detections.size.toString()
    }

    private fun displayChart(campaigns: List<Campaign>, detections: List<Detection>) {
        val campaignMap = campaigns.associateBy { it.id }
        val detectionCounts = detections.groupBy { it.campaignId }
            .mapValues { it.value.size }

        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        campaigns.take(10).forEachIndexed { index, campaign ->
            val count = detectionCounts[campaign.id] ?: 0
            entries.add(BarEntry(index.toFloat(), count.toFloat()))
            labels.add(campaign.title.take(15))
        }

        if (entries.isEmpty()) {
            binding.barChart.visibility = View.GONE
            return
        }

        val dataSet = BarDataSet(entries, "Detections per Campaign").apply {
            color = Color.parseColor("#FF6B6B")
            valueTextColor = Color.BLACK
            valueTextSize = 10f
        }

        val barData = BarData(dataSet)
        binding.barChart.data = barData

        binding.barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setFitBars(true)
            animateY(1000)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(labels)
                setDrawGridLines(false)
                textSize = 9f
                labelRotationAngle = -45f
            }

            axisLeft.apply {
                granularity = 1f
                setDrawGridLines(true)
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
        }

        binding.barChart.invalidate()
    }

    private suspend fun displayDetections(campaigns: List<Campaign>, detections: List<Detection>) {
        val campaignMap = campaigns.associateBy { it.id }
        
        val items = detections.take(20).mapNotNull { detection ->
            val campaign = campaignMap[detection.campaignId]
            campaign?.let {
                val userResult = repository.getUserProfile(detection.userId)
                val userEmail = if (userResult is Result.Success) {
                    userResult.data?.email ?: "Unknown User"
                } else {
                    "Unknown User"
                }
                
                DetectionItem(
                    detection = detection,
                    campaignTitle = campaign.title,
                    userEmail = userEmail
                )
            }
        }

        detectionAdapter.submitList(items)
    }

    private fun handleError(campaignsResult: Result<*>, detectionsResult: Result<*>) {
        val errorMessage = when {
            campaignsResult is Result.Failure -> 
                "Failed to load campaigns: ${campaignsResult.exception.message}"
            detectionsResult is Result.Failure -> 
                "Failed to load detections: ${detectionsResult.exception.message}"
            else -> "Unknown error occurred"
        }
        
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        showNoData()
    }

    private fun showNoData() {
        binding.layoutContent.visibility = View.GONE
        binding.tvNoData.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
