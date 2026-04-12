package com.phishing.simulation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.phishing.simulation.auth.AuthManager
import com.phishing.simulation.auth.AuthResult
import com.phishing.simulation.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // If a valid session already exists, skip the login screen entirely.
        if (authManager.isLoggedIn()) {
            checkRoleAndNavigate()
            return
        }

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (!validateInputs(email, password)) return

        setLoading(true)
        lifecycleScope.launch {
            val result = authManager.loginUser(email, password)
            setLoading(false)
            when (result) {
                is AuthResult.Success -> navigateByRole(result.role)
                is AuthResult.Failure -> showError(result.message)
            }
        }
    }

    /** Resolves the role of an already-authenticated user and navigates. */
    private fun checkRoleAndNavigate() {
        setLoading(true)
        lifecycleScope.launch {
            val role = authManager.getCurrentUserRole() ?: AuthManager.ROLE_VIEWER
            setLoading(false)
            navigateByRole(role)
        }
    }

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    private fun navigateByRole(role: String) {
        val destination = if (role == AuthManager.ROLE_ADMIN) {
            AdminMainActivity::class.java
        } else {
            UserMainActivity::class.java
        }
        startActivity(Intent(this, destination).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    private fun validateInputs(email: String, password: String): Boolean {
        var valid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email address"
            valid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            valid = false
        } else {
            binding.tilPassword.error = null
        }

        return valid
    }

    // -----------------------------------------------------------------------
    // UI helpers
    // -----------------------------------------------------------------------

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
