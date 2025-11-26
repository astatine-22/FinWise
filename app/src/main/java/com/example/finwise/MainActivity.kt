package com.example.finwise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.finwise.api.RetrofitClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var viewPager: ViewPager2
    private lateinit var tvUserNameHeader: android.widget.TextView
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userEmail = intent.getStringExtra("USER_EMAIL")
        tvUserNameHeader = findViewById(R.id.tvUserNameHeader)
        bottomNav = findViewById(R.id.bottomNavigationView)
        viewPager = findViewById(R.id.viewPager)
        val fabMainAdd = findViewById<FloatingActionButton>(R.id.fabMainAdd)
        val ivNotification: ImageView = findViewById(R.id.ivNotification)

        userEmail?.let { fetchHeaderInfo(it) }

        setupViewPager()
        setupBottomNavClicks()
        setupFabClick(fabMainAdd)
        setupLogoutClick(ivNotification)
    }

    private fun setupViewPager() {
        val adapter = MainViewPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.getChildAt(0).overScrollMode = androidx.recyclerview.widget.RecyclerView.OVER_SCROLL_NEVER

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val menuIndex = when(position) {
                    0 -> 0 // Home
                    1 -> 1 // Budget
                    2 -> 2 // Learn
                    3 -> 3 // Profile
                    else -> 0
                }
                bottomNav.menu.getItem(menuIndex).isChecked = true
            }
        })
    }

    private fun setupBottomNavClicks() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    viewPager.currentItem = 0
                    true
                }
                R.id.nav_budget -> {
                    viewPager.currentItem = 1
                    true
                }
                R.id.nav_learn -> {
                    viewPager.currentItem = 2
                    true
                }
                R.id.nav_profile -> {
                    viewPager.currentItem = 3
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFabClick(fab: FloatingActionButton) {
        fab.setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
        }
    }

    // --- THE FIX IS HERE ---
    private fun setupLogoutClick(ivNotification: ImageView) {
        ivNotification.setOnClickListener {
            // 1. CLEAR SAVED DATA
            val sharedPref = getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
            sharedPref.edit().remove("LOGGED_IN_EMAIL").apply()

            // 2. NAVIGATE TO LOGIN
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
    }
    // -----------------------

    private fun fetchHeaderInfo(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userProfile = RetrofitClient.instance.getUserDetails(email)
                withContext(Dispatchers.Main) {
                    tvUserNameHeader.text = userProfile.name
                }
            } catch (e: Exception) {
                // Handle error silently for header
            }
        }
    }
}