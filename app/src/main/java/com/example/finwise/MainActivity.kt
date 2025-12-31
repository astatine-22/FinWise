package com.example.finwise

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
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
    private lateinit var tvScreenTitle: TextView
    private lateinit var ivProfile: ImageView
    private lateinit var btnSearch: ImageView
    private var userEmail: String? = null

    // Screen titles for each tab
    private val screenTitles = listOf("Dashboard", "Budget", "Learn", "Trade")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userEmail = intent.getStringExtra("USER_EMAIL")

        tvScreenTitle = findViewById(R.id.tvScreenTitle)
        ivProfile = findViewById(R.id.ivProfile)
        btnSearch = findViewById(R.id.btnSearch)
        bottomNav = findViewById(R.id.bottomNavigationView)
        viewPager = findViewById(R.id.viewPager)
        val fabMainAdd = findViewById<FloatingActionButton>(R.id.fabMainAdd)

        userEmail?.let { fetchProfilePicture(it) }

        setupViewPager()
        setupBottomNavClicks()
        setupFabClick(fabMainAdd)
        setupProfileClick()
        setupSearchClick()
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile picture when returning from ProfileActivity
        userEmail?.let { fetchProfilePicture(it) }
    }

    private fun setupProfileClick() {
        ivProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupSearchClick() {
        btnSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupViewPager() {
        val adapter = MainViewPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.getChildAt(0).overScrollMode = androidx.recyclerview.widget.RecyclerView.OVER_SCROLL_NEVER

        val headerLayout = findViewById<android.widget.LinearLayout>(R.id.headerLayout)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                
                // Update screen title based on current tab
                tvScreenTitle.text = screenTitles.getOrElse(position) { "FinWise" }
                
                // Show search button only on Trade tab (position 3)
                btnSearch.visibility = if (position == 3) android.view.View.VISIBLE else android.view.View.GONE
                
                val menuIndex = when(position) {
                    0 -> 0 // Home
                    1 -> 1 // Budget
                    2 -> 2 // Learn
                    3 -> 3 // Trade
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
                R.id.nav_trade -> {
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

    private fun fetchProfilePicture(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userProfile = RetrofitClient.instance.getUserDetails(email)
                withContext(Dispatchers.Main) {
                    loadProfilePicture(userProfile.profile_picture)
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    private fun loadProfilePicture(base64Image: String?) {
        if (base64Image != null && base64Image.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ivProfile.setImageBitmap(bitmap)
                ivProfile.scaleType = ImageView.ScaleType.CENTER_CROP
                ivProfile.setPadding(0, 0, 0, 0)
                ivProfile.imageTintList = null
            } catch (e: Exception) {
                // Keep default icon on error
            }
        }
    }
}