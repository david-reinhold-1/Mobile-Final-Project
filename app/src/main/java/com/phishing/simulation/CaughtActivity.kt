package com.phishing.simulation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.phishing.simulation.databinding.ActivityCaughtBinding

class CaughtActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaughtBinding
    private var phishingUrl: String? = null

    companion object {
        const val EXTRA_PHISHING_URL = "phishing_url"
        private const val TAG = "CaughtActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaughtBinding.inflate(layoutInflater)
        setContentView(binding.root)

        phishingUrl = intent.getStringExtra(EXTRA_PHISHING_URL)

        binding.btnContinue.setOnClickListener {
            openPhishingPage()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun openPhishingPage() {
        if (phishingUrl.isNullOrBlank()) {
            Toast.makeText(this, "No URL provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(phishingUrl))
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Failed to open link: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            Log.e(TAG, "Failed to open phishing link", e)
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
