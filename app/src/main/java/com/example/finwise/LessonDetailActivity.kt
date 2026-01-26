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
    private lateinit var cardQuiz: androidx.cardview.widget.CardView
    private lateinit var tvQuizHeader: TextView
    private lateinit var tvQuestionCounter: TextView
    private lateinit var tvQuestionText: TextView
    private lateinit var radioGroupOptions: android.widget.RadioGroup
    private lateinit var rbOptionA: android.widget.RadioButton
    private lateinit var rbOptionB: android.widget.RadioButton
    private lateinit var rbOptionC: android.widget.RadioButton
    private lateinit var rbOptionD: android.widget.RadioButton
    private lateinit var btnNextConfirm: Button
    private lateinit var progressQuiz: ProgressBar
    private lateinit var tvQuizError: TextView

    // Quiz State
    private var quizQuestions: List<com.example.finwise.api.Question> = emptyList()
    private var currentQuestionIndex = 0
    private val currentAnswers = mutableMapOf<Int, String>() // QuestionID -> SelectedOption

    // Data
    private var videoId: Int = 0
    private var quizId: Int = 0
    private lateinit var sessionManager: SessionManager

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
        layoutQuizSection = findViewById(R.id.cardQuiz) // Use card as section for visibility
        cardQuiz = findViewById(R.id.cardQuiz)
        tvQuizHeader = findViewById(R.id.tvQuizHeader)
        tvQuestionCounter = findViewById(R.id.tvQuestionCounter)
        tvQuestionText = findViewById(R.id.tvQuestionText)
        radioGroupOptions = findViewById(R.id.radioGroupOptions)
        rbOptionA = findViewById(R.id.rbOptionA)
        rbOptionB = findViewById(R.id.rbOptionB)
        rbOptionC = findViewById(R.id.rbOptionC)
        rbOptionD = findViewById(R.id.rbOptionD)
        btnNextConfirm = findViewById(R.id.btnNextConfirm)
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

        // Setup WebView
        setupWebView(convertToEmbedUrl(videoUrl))

        // Fetch quiz
        fetchQuiz()

        // Listeners
        btnMarkCompleted.setOnClickListener { claimXP() }

        btnNextConfirm.setOnClickListener {
            handleNextClick()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun handleNextClick() {
        // Validate selection
        val selectedId = radioGroupOptions.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
            return
        }

        // Save answer
        val selectedRb = findViewById<android.widget.RadioButton>(selectedId)
        val selectedOption = when (selectedRb) {
            rbOptionA -> "A"
            rbOptionB -> "B"
            rbOptionC -> "C"
            rbOptionD -> "D"
            else -> "A"
        }

        val currentQuestion = quizQuestions[currentQuestionIndex]
        currentAnswers[currentQuestion.id] = selectedOption

        // Move to next
        if (currentQuestionIndex < quizQuestions.size - 1) {
            currentQuestionIndex++
            showQuestion(currentQuestionIndex)
        } else {
            // Submit
            submitQuiz()
        }
    }

    private fun showQuestion(index: Int) {
        val question = quizQuestions[index]

        tvQuestionCounter.text = "Question ${index + 1} of ${quizQuestions.size}"
        tvQuestionText.text = question.question_text

        rbOptionA.text = question.option_a
        rbOptionB.text = question.option_b
        rbOptionC.text = question.option_c
        rbOptionD.text = question.option_d

        // Reset selection
        radioGroupOptions.clearCheck()

        // Update Button Text
        if (index == quizQuestions.size - 1) {
            btnNextConfirm.text = "Finish Quiz"
        } else {
            btnNextConfirm.text = "Next Question"
        }
    }

    // ... (convertToEmbedUrl, setupWebView, claimXP from previous code) ...
    // Note: I need to preserve them or re-write them. I will re-include them for completeness if I replace the whole file or matching block. 
    // The previous tool call shows I am replacing the content. I should be careful. 
    // The previous view shows the file has many methods. 
    // I will use START_LINE/END_LINE replacement for the class body or specific methods to be safe.

    // Actually, I will replace the onCreate to the end of the class to update fields and logic.
    // BUT I need to keep convertingUrl and setupWebView logic. 

    // Let's replace specifically the fields and onCreate, and then the quiz methods.

    // I will assume I need to replace from line 33 (Quiz views) down to end.

    // Wait, rewriting convertingUrl etc is redundant if I can just replace the logic methods.

    // I'll update the whole class body logic in chunks? No, MultiReplace is better.

    private fun convertToEmbedUrl(url: String): String {
        if (url.contains("/embed/")) return url
        val videoId = if (url.contains("watch?v=")) url.substringAfter("watch?v=")
            .substringBefore("&") else url
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
        webView.loadUrl(videoUrl)
    }

    private fun claimXP() {
        val sharedPref = getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        val email = sharedPref.getString("LOGGED_IN_EMAIL", null)
        if (email == null) return

        btnMarkCompleted.isEnabled = false
        btnMarkCompleted.text = "Claiming XP..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response =
                    RetrofitClient.instance.completeLesson(LessonCompleteRequest(email, videoId))
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LessonDetailActivity, "XP Earned!", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnMarkCompleted.isEnabled = true
                    btnMarkCompleted.text = "Mark as Completed"
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish(); return true
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
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
                    showQuizError("No quiz for this lesson", showFallbackButton = true)
                }
            }
        }
    }

    private fun displayQuiz(quiz: Quiz) {
        quizId = quiz.id
        quizQuestions = quiz.questions
        currentQuestionIndex = 0
        currentAnswers.clear()

        cardQuiz.visibility = View.VISIBLE
        btnMarkCompleted.visibility = View.GONE

        if (quizQuestions.isNotEmpty()) {
            showQuestion(0)
        } else {
            showQuizError("No questions found", showFallbackButton = true)
        }
    }

    private fun submitQuiz() {
        val userEmail = sessionManager.fetchUserEmail()
        if (userEmail.isNullOrBlank()) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show()
            return
        }

        btnNextConfirm.isEnabled = false
        btnNextConfirm.text = "Submitting..."

        // Construct submission
        val answersMap = currentAnswers.mapKeys { it.key.toString() }
        val submission = QuizSubmission(userEmail, quizId, answersMap)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = RetrofitClient.instance.submitQuiz(submission)
                withContext(Dispatchers.Main) {
                    showResultDialog(
                        result.correct_count,
                        result.total_questions,
                        result.xp_earned,
                        result.message
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnNextConfirm.isEnabled = true
                    btnNextConfirm.text = "Finish Quiz"
                    Toast.makeText(
                        this@LessonDetailActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showResultDialog(
        correctCount: Int,
        totalQuestions: Int,
        xpEarned: Int,
        message: String
    ) {
        val dialogMessage =
            "You scored $correctCount/$totalQuestions\nXP Earned: $xpEarned\n\n$message"
        AlertDialog.Builder(this)
            .setTitle("🎉 Quiz Results")
            .setMessage(dialogMessage)
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
                finish() // Or reset? Usually finish.
            }
            .setCancelable(false)
            .show()
    }

    private fun showQuizLoading(show: Boolean) {
        progressQuiz.visibility = if (show) View.VISIBLE else View.GONE
        tvQuizError.visibility = View.GONE
        if (show) cardQuiz.visibility = View.GONE
    }

    private fun showQuizError(message: String, showFallbackButton: Boolean = false) {
        progressQuiz.visibility = View.GONE
        if (showFallbackButton) {
            tvQuizError.visibility = View.GONE
            cardQuiz.visibility = View.GONE
            btnMarkCompleted.visibility = View.VISIBLE
        } else {
            tvQuizError.visibility = View.VISIBLE
            tvQuizError.text = message
            cardQuiz.visibility = View.GONE
        }
    }
}
