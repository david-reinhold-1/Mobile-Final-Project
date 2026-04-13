package com.phishing.simulation

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.phishing.simulation.databinding.ActivityProfileBinding
import com.phishing.simulation.repository.FirebaseRepository
import com.phishing.simulation.repository.Result
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val repository = FirebaseRepository()
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDepartmentDropdown()
        loadUserProfile()

        binding.btnSave.setOnClickListener { saveProfile() }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        
        val btnBack = binding.toolbar.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBack)
        btnBack?.setOnClickListener {
            finish()
        }
    }

    private fun setupDepartmentDropdown() {
        val departments = arrayOf("IT", "HR", "Finance", "Marketing", "Sales", "Operations", "Engineering")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, departments)
        binding.actvDepartment.setAdapter(adapter)
    }

    private fun loadUserProfile() {
        userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            when (val result = repository.getUserProfile(userId!!)) {
                is Result.Success -> {
                    setLoading(false)
                    val user = result.data
                    if (user != null) {
                        binding.etName.setText(user.name)
                        binding.etEmail.setText(user.email)
                        binding.actvDepartment.setText(user.department, false)
                        binding.etRole.setText(user.role)
                    } else {
                        Toast.makeText(
                            this@ProfileActivity,
                            "User profile not found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                is Result.Failure -> {
                    setLoading(false)
                    Toast.makeText(
                        this@ProfileActivity,
                        "Error loading profile: ${result.exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val department = binding.actvDepartment.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            return
        }
        binding.tilName.error = null

        if (department.isEmpty()) {
            binding.tilDepartment.error = "Department is required"
            return
        }
        binding.tilDepartment.error = null

        setLoading(true)
        lifecycleScope.launch {
            val updates = mapOf(
                "Name" to name,
                "Department" to department
            )

            when (val result = repository.updateUserProfile(userId!!, updates)) {
                is Result.Success -> {
                    setLoading(false)
                    Toast.makeText(
                        this@ProfileActivity,
                        getString(R.string.profile_updated_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                is Result.Failure -> {
                    setLoading(false)
                    Toast.makeText(
                        this@ProfileActivity,
                        "Failed to update profile: ${result.exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !loading
    }
}
