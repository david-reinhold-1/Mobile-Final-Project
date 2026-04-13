package com.phishing.simulation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.phishing.simulation.auth.AuthManager
import com.phishing.simulation.auth.AuthResult
import com.phishing.simulation.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDepartmentDropdown()
        
        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.tvGoToLogin.setOnClickListener { finish() }
    }

    private fun setupDepartmentDropdown() {
        val departments = arrayOf("IT", "HR", "Finance", "Marketing", "Sales", "Operations", "Engineering")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, departments)
        binding.actvDepartment.setAdapter(adapter)
    }

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    private fun attemptRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val department = binding.actvDepartment.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        if (!validateInputs(name, email, department, password, confirmPassword)) return

        setLoading(true)
        lifecycleScope.launch {
            val result = authManager.registerUser(name, email, password, department)
            setLoading(false)
            when (result) {
                is AuthResult.Success -> onRegistrationSuccess()
                is AuthResult.Failure -> showError(result.message)
            }
        }
    }

    private fun onRegistrationSuccess() {
        Toast.makeText(this, "Account created! Please log in.", Toast.LENGTH_SHORT).show()
        // Return to LoginActivity and clear this back-stack entry
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    private fun validateInputs(
        name: String,
        email: String,
        department: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var valid = true

        if (name.isEmpty()) {
            binding.tilName.error = "Full name is required"
            valid = false
        } else {
            binding.tilName.error = null
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email address"
            valid = false
        } else {
            binding.tilEmail.error = null
        }

        if (department.isEmpty()) {
            binding.tilDepartment.error = "Department is required"
            valid = false
        } else {
            binding.tilDepartment.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            valid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            valid = false
        } else {
            binding.tilPassword.error = null
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            valid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            valid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return valid
    }

    // -----------------------------------------------------------------------
    // UI helpers
    // -----------------------------------------------------------------------

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
