package com.example.finwise

import android.content.Intent
import android.os.Bundle
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
        // Bell icon finding removed from here

        userEmail?.let { fetchHeaderInfo(it) }

        setupViewPager()
        setupBottomNavClicks()
        setupFabClick(fabMainAdd)
        // Logout setup call removed from here
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

    // The setupLogoutClick function is completely removed from here.

    private fun fetchHeaderInfo(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userProfile = RetrofitClient.instance.getUserDetails(email)
                withContext(Dispatchers.Main) {
                    tvUserNameHeader.text = userProfile.name
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}