package com.example.finwise

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // ========== FIX STATUS BAR ICONS FOR DARK MODE ==========
        // Detect if device is in Dark Mode
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        // Configure status bar icon colors
        WindowInsetsControllerCompat(window, window.decorView).apply {
            // false = Dark status bar background → WHITE icons (visible in Dark Mode)
            // true = Light status bar background → BLACK icons (visible in Light Mode)
            isAppearanceLightStatusBars = !isNightMode
        }
        // ========================================================

        // Load the ProfileFragment into the container
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ProfileFragment())
                .commit()
        }
    }
}
