package com.phishing.simulation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.phishing.simulation.auth.AuthManager
import com.phishing.simulation.databinding.ActivityUserMainBinding

class UserMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserMainBinding
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignOut.setOnClickListener {
            authManager.signOut()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }
}
