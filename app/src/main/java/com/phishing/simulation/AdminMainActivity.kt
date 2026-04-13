package com.phishing.simulation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.phishing.simulation.adapter.CampaignAdapter
import com.phishing.simulation.auth.AuthManager
import com.phishing.simulation.databinding.ActivityAdminMainBinding
import com.phishing.simulation.databinding.DialogCreateCampaignBinding
import com.phishing.simulation.model.Campaign
import com.phishing.simulation.repository.FirebaseRepository
import com.phishing.simulation.repository.Result
import kotlinx.coroutines.launch

class AdminMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMainBinding
    private val authManager = AuthManager()
    private val repository = FirebaseRepository()
    private lateinit var adapter: CampaignAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeCampaigns()

        binding.fabNewCampaign.setOnClickListener { showCreateCampaignDialog() }
    }

    // -----------------------------------------------------------------------
    // Toolbar
    // -----------------------------------------------------------------------

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        
        // Set up toolbar buttons
        val btnStatistics = binding.toolbar.findViewById<MaterialButton>(R.id.btnStatistics)
        val btnSignOut = binding.toolbar.findViewById<MaterialButton>(R.id.btnSignOut)
        
        btnStatistics?.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }
        
        btnSignOut?.setOnClickListener {
            authManager.signOut()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    // -----------------------------------------------------------------------
    // RecyclerView
    // -----------------------------------------------------------------------

    private fun setupRecyclerView() {
        adapter = CampaignAdapter(
            onDeleteClick = { campaign -> confirmDelete(campaign) },
            onEditClick = { campaign -> showEditCampaignDialog(campaign) }
        )
        binding.rvCampaigns.layoutManager = LinearLayoutManager(this)
        binding.rvCampaigns.adapter = adapter
    }

    // -----------------------------------------------------------------------
    // Real-time campaign stream
    // -----------------------------------------------------------------------

    private fun observeCampaigns() {
        setLoading(true)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.getAllCampaigns().collect { result ->
                    setLoading(false)
                    when (result) {
                        is Result.Success -> {
                            adapter.submitList(result.data)
                            binding.tvEmpty.visibility =
                                if (result.data.isEmpty()) View.VISIBLE else View.GONE
                            binding.rvCampaigns.visibility =
                                if (result.data.isEmpty()) View.GONE else View.VISIBLE
                        }
                        is Result.Failure -> {
                            Toast.makeText(
                                this@AdminMainActivity,
                                "Error loading campaigns: ${result.exception.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Create campaign dialog
    // -----------------------------------------------------------------------

    private fun showCreateCampaignDialog() {
        showCampaignDialog(null)
    }

    private fun showEditCampaignDialog(campaign: Campaign) {
        showCampaignDialog(campaign)
    }

    private fun showCampaignDialog(existingCampaign: Campaign?) {
        val dialogBinding = DialogCreateCampaignBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        // Setup department dropdown
        val departments = arrayOf("All", "IT", "HR", "Finance", "Marketing", "Sales", "Operations", "Engineering")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, departments)
        dialogBinding.actvDepartment.setAdapter(adapter)

        // If editing, pre-fill fields
        if (existingCampaign != null) {
            dialogBinding.etTitle.setText(existingCampaign.title)
            dialogBinding.etBody.setText(existingCampaign.description)
            dialogBinding.etUrl.setText(existingCampaign.landingPageUrl)
            dialogBinding.actvDepartment.setText(existingCampaign.department.ifEmpty { "All" }, false)
            dialogBinding.btnCreate.text = "Update"
        } else {
            dialogBinding.actvDepartment.setText("All", false)
        }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnCreate.setOnClickListener {
            val title = dialogBinding.etTitle.text.toString().trim()
            val body = dialogBinding.etBody.text.toString().trim()
            val url = dialogBinding.etUrl.text.toString().trim()
            val department = dialogBinding.actvDepartment.text.toString().trim().ifEmpty { "All" }

            if (!validateCampaignInputs(dialogBinding, title, body, url)) return@setOnClickListener

            dialogBinding.btnCreate.isEnabled = false
            dialogBinding.btnCancel.isEnabled = false

            val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: "unknown"
            val campaign = Campaign(
                id = existingCampaign?.id ?: "",
                title = title,
                description = body,
                landingPageUrl = url,
                department = department,
                createdBy = existingCampaign?.createdBy ?: currentUserEmail,
                createdAt = existingCampaign?.createdAt ?: Timestamp.now()
            )

            lifecycleScope.launch {
                when (val result = repository.createCampaign(campaign)) {
                    is Result.Success -> {
                        Toast.makeText(
                            this@AdminMainActivity,
                            if (existingCampaign != null) "Campaign updated!" else getString(R.string.campaign_created_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                    is Result.Failure -> {
                        dialogBinding.btnCreate.isEnabled = true
                        dialogBinding.btnCancel.isEnabled = true
                        Toast.makeText(
                            this@AdminMainActivity,
                            "Failed: ${result.exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }

    // -----------------------------------------------------------------------
    // Delete campaign
    // -----------------------------------------------------------------------

    private fun confirmDelete(campaign: Campaign) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_campaign_title))
            .setMessage(getString(R.string.delete_campaign_message, campaign.title))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ -> deleteCampaign(campaign) }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun deleteCampaign(campaign: Campaign) {
        lifecycleScope.launch {
            when (val result = repository.deleteCampaign(campaign.id)) {
                is Result.Success -> Toast.makeText(
                    this@AdminMainActivity,
                    getString(R.string.campaign_deleted_success),
                    Toast.LENGTH_SHORT
                ).show()
                is Result.Failure -> Toast.makeText(
                    this@AdminMainActivity,
                    "Delete failed: ${result.exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    private fun validateCampaignInputs(
        b: DialogCreateCampaignBinding,
        title: String,
        body: String,
        url: String
    ): Boolean {
        var valid = true

        if (title.isEmpty()) {
            b.tilTitle.error = "Campaign title is required"
            valid = false
        } else {
            b.tilTitle.error = null
        }

        if (body.isEmpty()) {
            b.tilBody.error = "Email body is required"
            valid = false
        } else {
            b.tilBody.error = null
        }

        if (url.isEmpty()) {
            b.tilUrl.error = "Phishing URL is required"
            valid = false
        } else if (!android.util.Patterns.WEB_URL.matcher(url).matches()) {
            b.tilUrl.error = "Enter a valid URL (e.g. https://example.com)"
            valid = false
        } else {
            b.tilUrl.error = null
        }

        return valid
    }

    // -----------------------------------------------------------------------
    // UI helpers
    // -----------------------------------------------------------------------

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
}

