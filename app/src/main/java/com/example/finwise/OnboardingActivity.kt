package com.example.finwise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {

    private lateinit var onboardingAdapter: OnboardingAdapter
    private lateinit var indicatorsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // KEEP THIS CHECK: If they logged out previously, they shouldn't see this again.
        val sharedPref = getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        val isOnboardingDone = sharedPref.getBoolean("COMPLETED_ONBOARDING", false)

        if (isOnboardingDone) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_onboarding)

        // Initialize Views
        val viewPager = findViewById<ViewPager2>(R.id.viewPagerOnboarding)
        indicatorsContainer = findViewById(R.id.layoutOnboardingIndicators)
        val btnAction = findViewById<MaterialButton>(R.id.btnAction)
        val tvSkip = findViewById<TextView>(R.id.tvSkip)

        // Data for the slides
        onboardingAdapter = OnboardingAdapter(
            listOf(
                OnboardingItem(
                    R.drawable.onboarding_1,
                    "Track Your Spending",
                    "Effortlessly track income and expenses to see exactly where your money goes."
                ),
                OnboardingItem(
                    R.drawable.onboarding_2,
                    "Learn & Grow",
                    "Master financial concepts with bite-sized lessons and interactive quizzes."
                ),
                OnboardingItem(
                    R.drawable.onboarding_3,
                    "Achieve Goals",
                    "Set savings targets, build better habits, and watch your wealth grow."
                )
            )
        )

        viewPager.adapter = onboardingAdapter

        setupIndicators()
        setCurrentIndicator(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)

                if (position == onboardingAdapter.itemCount - 1) {
                    btnAction.text = "Get Started"
                } else {
                    btnAction.text = "Next"
                }
            }
        })

        // --- SKIP BUTTON ACTION ---
        tvSkip.setOnClickListener {
            // Just go to login, don't save status yet
            navigateToLogin()
        }

        // --- MAIN BUTTON ACTION ---
        btnAction.setOnClickListener {
            if (viewPager.currentItem + 1 < onboardingAdapter.itemCount) {
                viewPager.currentItem += 1
            } else {
                // Just go to login, don't save status yet
                navigateToLogin()
            }
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setupIndicators() {
        val indicators = arrayOfNulls<ImageView>(onboardingAdapter.itemCount)
        val layoutParams: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        layoutParams.setMargins(8, 0, 8, 0)

        for (i in indicators.indices) {
            indicators[i] = ImageView(applicationContext)
            indicators[i]?.let {
                it.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.indicator_inactive
                    )
                )
                it.layoutParams = layoutParams
                indicatorsContainer.addView(it)
            }
        }
    }

    private fun setCurrentIndicator(index: Int) {
        val childCount = indicatorsContainer.childCount
        for (i in 0 until childCount) {
            val imageView = indicatorsContainer.getChildAt(i) as ImageView
            if (i == index) {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.indicator_active
                    )
                )
            } else {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.indicator_inactive
                    )
                )
            }
        }
    }
}