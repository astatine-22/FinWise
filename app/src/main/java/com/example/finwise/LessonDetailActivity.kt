package com.example.finwise

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LessonDetailActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var tvLessonTitle: TextView
    private lateinit var tvLessonDescription: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_detail)

        // Initialize views
        webView = findViewById(R.id.webView)
        tvLessonTitle = findViewById(R.id.tvLessonTitle)
        tvLessonDescription = findViewById(R.id.tvLessonDescription)

        // Get data from Intent
        val videoUrl = intent.getStringExtra("VIDEO_URL") ?: ""
        val title = intent.getStringExtra("TITLE") ?: "Lesson"
        val subtitle = intent.getStringExtra("SUBTITLE") ?: ""

        // Set title and description
        tvLessonTitle.text = title
        tvLessonDescription.text = subtitle

        // Setup WebView
        setupWebView(videoUrl)

        // Add back button support
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
