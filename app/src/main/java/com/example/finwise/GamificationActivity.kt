package com.example.finwise

import android.content.Context
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.AchievementDef
import com.example.finwise.api.GamificationResponse
import com.example.finwise.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity showing detailed gamification progress:
 * - XP and level progress
 * - Streak count and next milestone
 * - All achievements (earned shown full color, unearned grayed out)
 */
class GamificationActivity : AppCompatActivity() {

    private lateinit var tvCurrentXp: TextView
    private lateinit var tvLevelBadge: TextView
    private lateinit var progressXp: ProgressBar
    private lateinit var tvXpToNext: TextView
    private lateinit var tvStreakCount: TextView
    private lateinit var tvStreakMilestone: TextView
    private lateinit var rvAchievements: RecyclerView

    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamification)

        // Get user email
        val sharedPref = getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)

        // Initialize RetrofitClient
        RetrofitClient.initialize(this)

        // Initialize views
        tvCurrentXp = findViewById(R.id.tvCurrentXp)
        tvLevelBadge = findViewById(R.id.tvLevelBadge)
        progressXp = findViewById(R.id.progressXp)
        tvXpToNext = findViewById(R.id.tvXpToNext)
        tvStreakCount = findViewById(R.id.tvStreakCount)
        tvStreakMilestone = findViewById(R.id.tvStreakMilestone)
        rvAchievements = findViewById(R.id.rvAchievements)

        // Set up RecyclerView
        rvAchievements.layoutManager = LinearLayoutManager(this)

        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Load data
        userEmail?.let { email ->
            loadGamificationData(email)
        } ?: run {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadGamificationData(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch gamification status and all achievements in parallel
                val gamification = RetrofitClient.instance.getUserGamification(email)
                val allAchievements = RetrofitClient.instance.getAllAchievements()

                withContext(Dispatchers.Main) {
                    updateUI(gamification, allAchievements)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@GamificationActivity,
                        "Failed to load data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun updateUI(gamification: GamificationResponse, achievements: List<AchievementDef>) {
        // XP & Level
        tvCurrentXp.text = "${gamification.xp} XP"
        tvLevelBadge.text = "Level ${gamification.level} - ${gamification.level_title}"
        
        // Progress bar
        val progress = (gamification.progress_to_next * 100).toInt()
        progressXp.progress = progress
        
        val xpNeeded = gamification.xp_for_next_level - gamification.xp
        tvXpToNext.text = "$xpNeeded XP to next level"

        // Streak
        tvStreakCount.text = gamification.current_streak.toString()
        
        // Streak milestone message
        val streakMessage = when {
            gamification.current_streak < 7 -> "🎯 Next: ${7 - gamification.current_streak} more days for Week Warrior badge!"
            gamification.current_streak < 30 -> "🔥 Amazing! ${30 - gamification.current_streak} more days for Consistency King!"
            else -> "👑 You're a Consistency King! Keep the streak going!"
        }
        tvStreakMilestone.text = streakMessage

        // Set up achievements adapter
        rvAchievements.adapter = AchievementsAdapter(achievements, gamification.earned_achievements)
    }
}
