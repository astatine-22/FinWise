package com.example.finwise

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: Button
    private lateinit var tvSkip: TextView
    private lateinit var dots: List<ImageView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // Initialize views
        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btnNext)
        tvSkip = findViewById(R.id.tvSkip)

        // Setup Dots indicators
        dots = listOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3)
        )

        // 1. Prepare Data using your real images
        val slides = listOf(
            OnboardingItem(
                R.drawable.onboarding_1, // The Plant Image
                "Master Your Money Early",
                "Gain financial confidence and build smart habits for your future."
            ),
            OnboardingItem(
                R.drawable.onboarding_2, // The Pie Chart Image
                "Track Spending Effortlessly",
                "See where your money goes and make smarter decisions."
            ),
            OnboardingItem(
                R.drawable.onboarding_3, // The Graph Image
                "Practice Investing With No Risk",
                "Experience the market with virtual money before you invest for real."
            )
        )

        // 2. Set Adapter
        val adapter = OnboardingAdapter(slides)
        viewPager.adapter = adapter

        // 3. Handle Swipe Changes (Update Dots & Button Text)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDots(position)

                // Change button text on the last slide
                if (position == slides.size - 1) {
                    btnNext.text = "Get Started"
                } else {
                    btnNext.text = "Next"
                }
            }
        })

        // 4. Button Actions
        btnNext.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem < slides.size - 1) {
                // Move to next slide
                viewPager.setCurrentItem(currentItem + 1, true)
            } else {
                // Last slide? Go to Login
                finishOnboarding()
            }
        }

        tvSkip.setOnClickListener {
            finishOnboarding()
        }

        // Initialize dots state at start
        updateDots(0)
    }

    // Helper function to change dot colors
    private fun updateDots(activePosition: Int) {
        // Using the FinWise green color
        val activeColor = Color.parseColor("#9ED9C5")
        val inactiveColor = Color.LTGRAY

        dots.forEachIndexed { index, imageView ->
            imageView.setColorFilter(if (index == activePosition) activeColor else inactiveColor)
        }
    }

    // Helper function to move to the Login Screen
    private fun finishOnboarding() {
        // Navigate to LOGIN ACTIVITY
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close onboarding
    }
}