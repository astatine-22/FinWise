package com.example.finwise

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity container for the LeaderboardFragment (Hall of Fame).
 * Uses a FrameLayout to host the fragment.
 */
class LeaderboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        // Load the fragment if not already present
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LeaderboardFragment())
                .commit()
        }
    }
}
