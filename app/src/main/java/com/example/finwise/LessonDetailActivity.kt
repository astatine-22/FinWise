package com.example.finwise

import android.content.Context
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.LessonCompleteRequest
import com.example.finwise.api.Quiz
import com.example.finwise.api.QuizSubmission
import com.example.finwise.api.RetrofitClient
import com.example.finwise.api.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LessonDetailActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var tvLessonTitle: TextView
    private lateinit var tvLessonDescription: TextView
    private lateinit var btnMarkCompleted: Button
    
    // Quiz-related views
    private lateinit var layoutQuizSection: View
    private lateinit var tvQuizTitle: TextView
    private lateinit var rvQuestions: RecyclerView
    private lateinit var btnSubmitQuiz: Button
    private lateinit var progressQuiz: ProgressBar
    private lateinit var tvQuizError: TextView
    
    // Data
    private var videoId: Int = 0
    private var quizId: Int = 0
    private lateinit var sessionManager: SessionManager
    private lateinit var quizAdapter: QuizAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_detail)

        // Initialize SessionManager
        sessionManager = SessionManager.getInstance(this)

        // Initialize views
        webView = findViewById(R.id.webView)
        tvLessonTitle = findViewById(R.id.tvLessonTitle)
        tvLessonDescription = findViewById(R.id.tvLessonDescription)
        btnMarkCompleted = findViewById(R.id.btnMarkCompleted)
        
        // Initialize quiz views
        layoutQuizSection = findViewById(R.id.layoutQuizSection)
        tvQuizTitle = findViewById(R.id.tvQuizTitle)
        rvQuestions = findViewById(R.id.rvQuestions)
        btnSubmitQuiz = findViewById(R.id.btnSubmitQuiz)
        progressQuiz = findViewById(R.id.progressQuiz)
        tvQuizError = findViewById(R.id.tvQuizError)

        // Get data from Intent
        val videoUrl = intent.getStringExtra("VIDEO_URL") ?: ""
        val title = intent.getStringExtra("TITLE") ?: "Lesson"
        val subtitle = intent.getStringExtra("SUBTITLE") ?: ""
        videoId = intent.getIntExtra("VIDEO_ID", 0)

        // Set title and description
        tvLessonTitle.text = title
        tvLessonDescription.text = subtitle

        // Setup WebView - convert regular YouTube URL to embed if needed
        setupWebView(convertToEmbedUrl(videoUrl))

        // Fetch quiz for this video
        fetchQuiz()

        // Set up completion button click listener
        btnMarkCompleted.setOnClickListener {
            claimXP()
        }
        
        // Set up quiz submit button
        btnSubmitQuiz.setOnClickListener {
            submitQuiz()
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
    
    private fun fetchQuiz() {
        if (videoId == 0) {
            showQuizError("Invalid video ID", showFallbackButton = true)
            return
        }

        showQuizLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val quiz: Quiz = RetrofitClient.instance.getQuiz(videoId)

                withContext(Dispatchers.Main) {
                    showQuizLoading(false)
                    displayQuiz(quiz)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showQuizLoading(false)
                    // If 404/not found, it means no quiz exists -> Show fallback button
                    showQuizError("No quiz for this lesson", showFallbackButton = true)
                }
            }
        }
    }

    private fun displayQuiz(quiz: Quiz) {
        quizId = quiz.id
        tvQuizTitle.text = quiz.title

        // Setup RecyclerView
        quizAdapter = QuizAdapter(quiz.questions)
        rvQuestions.apply {
            layoutManager = LinearLayoutManager(this@LessonDetailActivity)
            adapter = quizAdapter
        }

        // Show quiz section
        layoutQuizSection.visibility = View.VISIBLE
        
        // SWITCH BUTTONS: Hide generic complete, Show Submit Quiz
        btnMarkCompleted.visibility = View.GONE
        btnSubmitQuiz.visibility = View.VISIBLE
        btnSubmitQuiz.isEnabled = true
    }

    private fun submitQuiz() {
        // Get user email
        val userEmail = sessionManager.fetchUserEmail()
        if (userEmail.isNullOrBlank()) {
            Toast.makeText(this, "Please log in to submit quiz", Toast.LENGTH_SHORT).show()
            return
        }

        // Collect answers from adapter
        val answers = quizAdapter.getAnswers()

        // Check if all questions are answered
        val totalQuestions = quizAdapter.itemCount
        if (answers.size < totalQuestions) {
            Toast.makeText(
                this,
                "Please answer all questions (${answers.size}/$totalQuestions answered)",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Disable button to prevent double submission
        btnSubmitQuiz.isEnabled = false
        btnSubmitQuiz.text = "Submitting..."

        // Submit quiz
        val submission = QuizSubmission(
            email = userEmail,
            quiz_id = quizId,
            answers = answers
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = RetrofitClient.instance.submitQuiz(submission)

                withContext(Dispatchers.Main) {
                    showResultDialog(result.correct_count, result.total_questions, result.xp_earned, result.message)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnSubmitQuiz.isEnabled = true
                    btnSubmitQuiz.text = "Submit Quiz"
                    Toast.makeText(
                        this@LessonDetailActivity,
                        "Failed to submit quiz: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showResultDialog(correctCount: Int, totalQuestions: Int, xpEarned: Int, message: String) {
        val dialogMessage = buildString {
            append("You scored $correctCount/$totalQuestions\n\n")
            append("XP Earned: $xpEarned\n\n")
            append(message)
        }

        AlertDialog.Builder(this)
            .setTitle("🎉 Quiz Results")
            .setMessage(dialogMessage)
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
                // Reset submit button
                btnSubmitQuiz.text = "Submit Quiz"
                btnSubmitQuiz.isEnabled = false
            }
            .setCancelable(false)
            .show()
    }

    private fun showQuizLoading(show: Boolean) {
        progressQuiz.visibility = if (show) View.VISIBLE else View.GONE
        tvQuizError.visibility = View.GONE
        if (show) {
            layoutQuizSection.visibility = View.GONE
            btnSubmitQuiz.visibility = View.GONE
            btnMarkCompleted.visibility = View.GONE
        }
    }

    private fun showQuizError(message: String, showFallbackButton: Boolean = false) {
        progressQuiz.visibility = View.GONE
        
        // Only show error text if meaningful, otherwise just hide section
        if (showFallbackButton) {
            // No quiz -> Show fallback button
            tvQuizError.visibility = View.GONE
            layoutQuizSection.visibility = View.GONE
            btnMarkCompleted.visibility = View.VISIBLE
            btnSubmitQuiz.visibility = View.GONE
        } else {
            // Error loading quiz -> Show error
            tvQuizError.visibility = View.VISIBLE
            tvQuizError.text = message
            layoutQuizSection.visibility = View.GONE
            btnSubmitQuiz.visibility = View.GONE
            // Keep generic button hidden or visible based on previous state? 
            // Better to show fallback if quiz fails really hard
            btnMarkCompleted.visibility = View.VISIBLE 
        }
    }
}
