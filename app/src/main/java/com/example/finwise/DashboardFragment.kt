package com.example.finwise

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.finwise.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class DashboardFragment : Fragment() {

    // Gamification views
    private lateinit var tvXp: TextView
    private lateinit var tvLevelTitle: TextView
    private lateinit var tvBadgesCount: TextView
    private lateinit var tvStreak: TextView
    
    // Budget views
    private lateinit var tvBudgetPercent: TextView
    private lateinit var progressBudget: ProgressBar
    private lateinit var tvNextLesson: TextView
    private lateinit var tvPortfolioValue: TextView
    
    private var userEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get User Email from shared preferences
        val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)

        // Initialize Gamification Views
        tvXp = view.findViewById(R.id.tvXp)
        tvLevelTitle = view.findViewById(R.id.tvLevelTitle)
        tvBadgesCount = view.findViewById(R.id.tvBadgesCount)
        tvStreak = view.findViewById(R.id.tvStreak)
        
        // Initialize Other Views
        tvBudgetPercent = view.findViewById(R.id.tvBudgetPercent)
        progressBudget = view.findViewById(R.id.progressBudget)
        tvNextLesson = view.findViewById(R.id.tvNextLesson)
        tvPortfolioValue = view.findViewById(R.id.tvPortfolioValue)
        
        val cardBudget = view.findViewById<CardView>(R.id.cardBudget)
        val cardLearn = view.findViewById<CardView>(R.id.cardLearn)
        val cardPaperTrading = view.findViewById<CardView>(R.id.cardPaperTrading)
        val cardSavingsGoals = view.findViewById<CardView>(R.id.cardSavingsGoals)
        
        // Gamification cards
        val cardXp = view.findViewById<CardView>(R.id.cardXp)
        val cardBadges = view.findViewById<CardView>(R.id.cardBadges)
        val cardStreak = view.findViewById<CardView>(R.id.cardStreak)

        // Load all data
        userEmail?.let { email ->
            fetchGamificationData(email)  // New gamification fetch
            fetchBudgetData(email)
            fetchLearnData()
            fetchPortfolioData(email)
        }

        // Gamification card click handlers - open GamificationActivity
        val openGamification = { 
            startActivity(android.content.Intent(requireContext(), GamificationActivity::class.java))
        }
        cardXp.setOnClickListener { openGamification() }
        cardBadges.setOnClickListener { openGamification() }
        cardStreak.setOnClickListener { openGamification() }

        // Navigation click listeners
        cardBudget.setOnClickListener {
            val viewPager = requireActivity().findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
            viewPager.currentItem = 1
        }

        cardLearn.setOnClickListener {
            val viewPager = requireActivity().findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
            viewPager.currentItem = 2
        }

        cardPaperTrading.setOnClickListener {
            val viewPager = requireActivity().findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
            viewPager.currentItem = 3
        }

        cardSavingsGoals.setOnClickListener {
            Toast.makeText(requireContext(), "Savings Goals coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Hall of Fame - Open LeaderboardActivity
        view.findViewById<CardView>(R.id.cardHallOfFame).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), LeaderboardActivity::class.java))
        }
    }

    private fun fetchGamificationData(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val gamification = RetrofitClient.instance.getUserGamification(email)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        tvXp.text = gamification.xp.toString()
                        tvLevelTitle.text = gamification.level_title
                        tvBadgesCount.text = gamification.earned_achievements.size.toString()
                        tvStreak.text = gamification.current_streak.toString()
                    }
                }
            } catch (e: Exception) {
                // Fallback to basic user data if gamification fails
                try {
                    val userProfile = RetrofitClient.instance.getUserDetails(email)
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            tvXp.text = userProfile.xp.toString()
                        }
                    }
                } catch (e2: Exception) {
                    // Handle error silently
                }
            }
        }
    }

    private fun fetchBudgetData(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val budgetSummary = RetrofitClient.instance.getBudgetSummary(email, "1m")
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        val limit = budgetSummary.limit
                        val spent = budgetSummary.total_spent
                        
                        if (limit > 0) {
                            val percent = ((spent / limit) * 100).toInt().coerceIn(0, 100)
                            tvBudgetPercent.text = "$percent% Used"
                            progressBudget.progress = percent
                        } else {
                            tvBudgetPercent.text = "No budget set"
                            progressBudget.progress = 0
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        tvBudgetPercent.text = "Set budget"
                        progressBudget.progress = 0
                    }
                }
            }
        }
    }

    private fun fetchLearnData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val videos = RetrofitClient.instance.getLearnVideos(null)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        if (videos.isNotEmpty()) {
                            val firstVideo = videos.first()
                            tvNextLesson.text = "Next: ${firstVideo.title}"
                        } else {
                            tvNextLesson.text = "No lessons available"
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        tvNextLesson.text = "Explore lessons"
                    }
                }
            }
        }
    }

    private fun fetchPortfolioData(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val portfolio = RetrofitClient.instance.getPortfolio(email)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                        val totalValue = portfolio.total_portfolio_value
                        tvPortfolioValue.text = formatter.format(totalValue)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        tvPortfolioValue.text = "₹1,00,000"
                    }
                }
            }
        }
    }
}