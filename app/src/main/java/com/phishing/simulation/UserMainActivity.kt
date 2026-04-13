package com.phishing.simulation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.phishing.simulation.auth.AuthManager
import com.phishing.simulation.databinding.ActivityUserMainBinding
import com.phishing.simulation.model.Campaign
import com.phishing.simulation.model.Detection
import com.phishing.simulation.repository.FirebaseRepository
import com.phishing.simulation.repository.Result
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserMainBinding
    private val authManager = AuthManager()
    private val repository = FirebaseRepository()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private var currentCampaign: Campaign? = null
    private var isDetectionRecorded = false

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                openPhishingLinkWithLocation()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                openPhishingLinkWithLocation()
            }
            else -> {
                Toast.makeText(
                    this,
                    getString(R.string.location_permission_denied),
                    Toast.LENGTH_LONG
                ).show()
                openPhishingLinkWithoutLocation()
            }
        }
    }

    companion object {
        private const val TAG = "UserMainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupToolbar()
        loadCampaigns()
        
        binding.btnOpenLink.setOnClickListener { handlePhishingLinkClick() }
        binding.btnSignOut.setOnClickListener {
            authManager.signOut()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun loadCampaigns() {
        setLoading(true)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.getAllCampaigns().collect { result ->
                    setLoading(false)
                    when (result) {
                        is Result.Success -> {
                            if (result.data.isNotEmpty()) {
                                currentCampaign = result.data.first()
                                displayCampaign(currentCampaign!!)
                            } else {
                                showNoCampaigns()
                            }
                        }
                        is Result.Failure -> {
                            Toast.makeText(
                                this@UserMainActivity,
                                "Error loading campaigns: ${result.exception.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            showNoCampaigns()
                        }
                    }
                }
            }
        }
    }

    private fun displayCampaign(campaign: Campaign) {
        binding.tvNoCampaigns.visibility = View.GONE
        binding.cardCampaign.visibility = View.VISIBLE
        
        binding.tvCampaignTitle.text = campaign.title
        binding.tvCampaignDescription.text = campaign.description
    }

    private fun showNoCampaigns() {
        binding.cardCampaign.visibility = View.GONE
        binding.tvNoCampaigns.visibility = View.VISIBLE
    }

    private fun handlePhishingLinkClick() {
        if (currentCampaign == null) {
            Toast.makeText(this, "No campaign available", Toast.LENGTH_SHORT).show()
            return
        }

        if (isDetectionRecorded) {
            openPhishingLink()
            return
        }

        if (hasLocationPermission()) {
            openPhishingLinkWithLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun openPhishingLinkWithLocation() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val location = getCurrentLocation()
                saveDetection(location)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get location", e)
                Toast.makeText(
                    this@UserMainActivity,
                    "Failed to get location. Recording without location.",
                    Toast.LENGTH_SHORT
                ).show()
                openPhishingLinkWithoutLocation()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun openPhishingLinkWithoutLocation() {
        lifecycleScope.launch {
            saveDetection(GeoPoint(0.0, 0.0))
        }
    }

    private suspend fun getCurrentLocation(): GeoPoint {
        if (!hasLocationPermission()) {
            throw SecurityException("Location permission not granted")
        }

        val cancellationTokenSource = CancellationTokenSource()
        
        try {
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()

            return if (location != null) {
                GeoPoint(location.latitude, location.longitude)
            } else {
                GeoPoint(0.0, 0.0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current location", e)
            throw e
        }
    }

    private suspend fun saveDetection(location: GeoPoint) {
        val campaign = currentCampaign ?: return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val detection = Detection(
            campaignId = campaign.id,
            userId = userId,
            location = location,
            timestamp = Timestamp.now()
        )

        when (val result = repository.saveDetection(detection)) {
            is Result.Success -> {
                isDetectionRecorded = true
                Log.d(TAG, "Detection saved successfully with ID: ${result.data}")
                openPhishingLink()
            }
            is Result.Failure -> {
                Toast.makeText(
                    this,
                    getString(R.string.detection_failed),
                    Toast.LENGTH_LONG
                ).show()
                Log.e(TAG, "Failed to save detection", result.exception)
            }
        }
    }

    private fun openPhishingLink() {
        val campaign = currentCampaign ?: return
        
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(campaign.landingPageUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Failed to open link: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            Log.e(TAG, "Failed to open phishing link", e)
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
