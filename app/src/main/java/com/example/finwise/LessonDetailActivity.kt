package com.example.finwise

import android.content.Context
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finwise.api.LessonCompleteRequest
import com.example.finwise.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LessonDetailActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var tvLessonTitle: TextView
    private lateinit var tvLessonDescription: TextView
    private lateinit var btnMarkCompleted: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_detail)

        // Initialize views
        webView = findViewById(R.id.webView)
        tvLessonTitle = findViewById(R.id.tvLessonTitle)
        tvLessonDescription = findViewById(R.id.tvLessonDescription)
        btnMarkCompleted = findViewById(R.id.btnMarkCompleted)

        // Get data from Intent
        val videoUrl = intent.getStringExtra("VIDEO_URL") ?: ""
        val title = intent.getStringExtra("TITLE") ?: "Lesson"
        val subtitle = intent.getStringExtra("SUBTITLE") ?: ""

        // Set title and description
        tvLessonTitle.text = title
        tvLessonDescription.text = subtitle

        // Setup WebView - convert regular YouTube URL to embed if needed
        setupWebView(convertToEmbedUrl(videoUrl))

        // Set up completion button click listener
        btnMarkCompleted.setOnClickListener {
            claimXP()
        }

        // Add back button support
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun convertToEmbedUrl(url: String): String {
        // If already an embed URL, return as is
        if (url.contains("/embed/")) {
            return url
        }
        
        // Extract video ID from watch URL (e.g., watch?v=VIDEO_ID)
        val videoId = if (url.contains("watch?v=")) {
            url.substringAfter("watch?v=").substringBefore("&")
        } else {
            // Assume it's already just a video ID or embed URL
            url
        }
        
        return "https://www.youtube.com/embed/$videoId"
    }

    private fun setupWebView(videoUrl: String) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        webView.webChromeClient = WebChromeClient()

        // Load the YouTube video
        webView.loadUrl(videoUrl)
    }

    private fun claimXP() {
        // Get user email from SharedPreferences
        val sharedPref = getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        val email = sharedPref.getString("LOGGED_IN_EMAIL", null)

        if (email == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button to prevent multiple clicks
        btnMarkCompleted.isEnabled = false
        btnMarkCompleted.text = "Claiming XP..."

        // Call the API to claim XP
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.completeLesson(
                    LessonCompleteRequest(email = email, video_id = 1)
                )
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LessonDetailActivity, "XP Earned! ${response.message}", Toast.LENGTH_SHORT).show()
                    finish() // Close activity and return to Learn screen
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LessonDetailActivity, "Failed to claim XP: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnMarkCompleted.isEnabled = true
                    btnMarkCompleted.text = "Mark as Completed (+100 XP)"
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
