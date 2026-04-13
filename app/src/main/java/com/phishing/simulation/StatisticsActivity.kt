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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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

class StatisticsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityStatisticsBinding
    private val repository = FirebaseRepository()
    private lateinit var detectionAdapter: DetectionAdapter
    private var googleMap: GoogleMap? = null
    private var detectionsForMap: List<Detection> = emptyList()
    private var campaignsMap: Map<String, Campaign> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupMap()
        loadStatistics()
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        googleMap?.uiSettings?.apply {
            isZoomControlsEnabled = true
            isZoomGesturesEnabled = true
            isScrollGesturesEnabled = true
            isTiltGesturesEnabled = false
            isRotateGesturesEnabled = false
        }
        
        displayDetectionsOnMap()
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

                    campaignsMap = campaigns.associateBy { it.id }
                    detectionsForMap = detections

                    if (campaigns.isEmpty() && detections.isEmpty()) {
                        showNoData()
                    } else {
                        displayStatistics(campaigns, detections)
                        displayChart(campaigns, detections)
                        displayDetections(campaigns, detections)
                        displayDetectionsOnMap()
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

    private fun displayDetectionsOnMap() {
        val map = googleMap ?: return
        
        if (detectionsForMap.isEmpty()) {
            binding.cardMap.visibility = View.GONE
            return
        }

        // Filter detections with valid locations (not 0,0)
        val validDetections = detectionsForMap.filter { detection ->
            detection.location.latitude != 0.0 || detection.location.longitude != 0.0
        }

        if (validDetections.isEmpty()) {
            binding.cardMap.visibility = View.GONE
            return
        }

        binding.cardMap.visibility = View.VISIBLE
        map.clear()

        val boundsBuilder = LatLngBounds.Builder()
        var hasValidMarkers = false

        validDetections.forEach { detection ->
            val latLng = LatLng(detection.location.latitude, detection.location.longitude)
            
            val campaign = campaignsMap[detection.campaignId]
            val title = campaign?.title ?: "Unknown Campaign"
            
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .snippet("Detection recorded")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            
            boundsBuilder.include(latLng)
            hasValidMarkers = true
        }

        if (hasValidMarkers) {
            try {
                val bounds = boundsBuilder.build()
                val padding = 100
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            } catch (e: Exception) {
                // If only one marker or bounds are invalid, just zoom to first location
                if (validDetections.isNotEmpty()) {
                    val firstLocation = LatLng(
                        validDetections.first().location.latitude,
                        validDetections.first().location.longitude
                    )
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 12f))
                }
            }
        }
    }
}
