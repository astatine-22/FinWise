package com.example.finwise

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.finwise.api.CheckAnswerRequest
import com.example.finwise.api.ClaimBonusRequest
import com.example.finwise.api.LessonCompleteRequest
import com.example.finwise.api.Quiz
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

    // Quiz Views
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

    // State
    private var quizQuestions: List<com.example.finwise.api.Question> = emptyList()
    private var currentQuestionIndex = 0
    private var isAnswerChecked = false
    private var correctCount = 0
    private var isVideoCompleted = false
    private var videoId: Int = 0 // DB ID
    private var quizId: Int = 0
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_detail)

        try {
            // 1. Initialize Session
            sessionManager = SessionManager.getInstance(this)

            // 2. Initialize Views
            initViews()

            // 3. Get Data from Intent
            val rawYoutubeId = intent.getStringExtra("YOUTUBE_ID") ?: ""
            val title = intent.getStringExtra("TITLE") ?: "Lesson"
            val subtitle = intent.getStringExtra("SUBTITLE") ?: ""
            videoId = intent.getIntExtra("VIDEO_ID", 0)

            Log.d("LessonDetail", "Received - YouTube URL: $rawYoutubeId, DB ID: $videoId")

            // 4. Setup UI
            tvLessonTitle.text = title
            tvLessonDescription.text = subtitle
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            // 5. Setup Video Player (using full YouTube URL from database)
            if (rawYoutubeId.isNotEmpty()) {
                Log.d("LessonDetail", "Loading video: $rawYoutubeId")
                setupWebViewPlayer(rawYoutubeId)
            } else {
                Log.e("LessonDetail", "No YouTube URL provided")
                Toast.makeText(this, "Error: No video URL", Toast.LENGTH_SHORT).show()
            }

            // 6. Initial Fetch
            fetchQuiz()

            // 7. Listeners
            setupListeners()

        } catch (e: Exception) {
            Log.e("LessonDetail", "Error in onCreate", e)
            Toast.makeText(this, "Error loading lesson", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initViews() {
        webView = findViewById(R.id.webview_player)
        tvLessonTitle = findViewById(R.id.tvLessonTitle)
        tvLessonDescription = findViewById(R.id.tvLessonDescription)
        btnMarkCompleted = findViewById(R.id.btnMarkCompleted)

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
    }

    private fun setupListeners() {
        btnMarkCompleted.setOnClickListener { claimXP() }

        btnNextConfirm.setOnClickListener {
            if (isAnswerChecked) {
                handleNextClick()
            } else {
                handleCheckAnswer()
            }
        }
    }

    private fun extractVideoId(url: String): String {
        return try {
            when {
                url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
                url.contains("/embed/") -> url.substringAfter("/embed/").substringBefore("?")
                url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
                else -> url // Assume raw ID
            }
        } catch (e: Exception) {
            Log.e("LessonDetail", "Error extracting ID", e)
            url
        }
    }

    private fun setupWebViewPlayer(videoInput: String) {
        // Configure WebView settings
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
        }

        // Add console logging
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d("WebView", "Console: ${it.message()} -- Line: ${it.lineNumber()}")
                }
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("LessonDetail", "Page loaded: $url")
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Log.e("LessonDetail", "WebView error: ${error?.description}")
            }
        }

        // Check if videoInput is already a full URL or just an ID
        val youtubeUrl = if (videoInput.startsWith("http")) {
            videoInput // Already a full URL
        } else {
            "https://www.youtube.com/watch?v=$videoInput" // Build URL from ID
        }
        
        Log.d("LessonDetail", "Loading YouTube URL: $youtubeUrl")
        webView.loadUrl(youtubeUrl)
    }

    // --- Quiz Logic ---

    private fun fetchQuiz() {
        if (videoId == 0) {
            showQuizError("Invalid Lesson ID", showFallbackButton = true)
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
                    showQuizError("No quiz available for this lesson", showFallbackButton = true)
                }
            }
        }
    }

    private fun displayQuiz(quiz: Quiz) {
        quizId = quiz.id
        quizQuestions = quiz.questions
        currentQuestionIndex = 0
        correctCount = 0
        isAnswerChecked = false

        cardQuiz.visibility = View.VISIBLE
        btnMarkCompleted.visibility = View.GONE

        if (quizQuestions.isNotEmpty()) {
            showQuestion(0)
        } else {
            showQuizError("No questions found", showFallbackButton = true)
        }
    }

    private fun showQuestion(index: Int) {
        isAnswerChecked = false
        val question = quizQuestions[index]

        tvQuestionCounter.text = "Question ${index + 1} of ${quizQuestions.size}"
        tvQuestionText.text = question.question_text

        rbOptionA.text = question.option_a
        rbOptionB.text = question.option_b
        rbOptionC.text = question.option_c
        rbOptionD.text = question.option_d

        radioGroupOptions.clearCheck()
        enableRadioButtons()
        resetRadioButtonColors()

        btnNextConfirm.text = "Check Answer"
        btnNextConfirm.isEnabled = true
    }

    private fun handleCheckAnswer() {
        val selectedId = radioGroupOptions.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRb = findViewById<android.widget.RadioButton>(selectedId)
        val selectedOption = when (selectedRb) {
            rbOptionA -> "A"
            rbOptionB -> "B"
            rbOptionC -> "C"
            rbOptionD -> "D"
            else -> "A"
        }

        val currentQuestion = quizQuestions[currentQuestionIndex]
        val userEmail = sessionManager.fetchUserEmail() ?: return

        btnNextConfirm.isEnabled = false
        btnNextConfirm.text = "Checking..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.checkAnswer(
                    CheckAnswerRequest(userEmail, currentQuestion.id, selectedOption)
                )

                withContext(Dispatchers.Main) {
                    btnNextConfirm.isEnabled = true
                    isAnswerChecked = true
                    btnNextConfirm.text = "Next Question"

                    if (response.is_correct) {
                        correctCount++
                        setOptionColor(selectedRb, true)
                        Toast.makeText(this@LessonDetailActivity, response.xp_message, Toast.LENGTH_SHORT).show()
                    } else {
                        setOptionColor(selectedRb, false)
                        highlightCorrectAnswer(response.correct_option)
                        Toast.makeText(this@LessonDetailActivity, "Wrong Answer", Toast.LENGTH_SHORT).show()
                    }
                    disableRadioButtons()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnNextConfirm.isEnabled = true
                    btnNextConfirm.text = "Check Answer"
                    Toast.makeText(this@LessonDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleNextClick() {
        if (currentQuestionIndex < quizQuestions.size - 1) {
            currentQuestionIndex++
            showQuestion(currentQuestionIndex)
        } else {
            handleQuizCompletion()
        }
    }

    private fun handleQuizCompletion() {
        val totalQuestions = quizQuestions.size
        val userEmail = sessionManager.fetchUserEmail() ?: return

        if (correctCount == totalQuestions) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val bonusResponse = RetrofitClient.instance.claimBonus(
                        ClaimBonusRequest(userEmail, quizId)
                    )
                    withContext(Dispatchers.Main) {
                        showResultDialog(correctCount, totalQuestions, bonusResponse.xp_bonus, "🏆 Perfect Score! Bonus XP Awarded!")
                    }
                } catch (e: Exception) {
                     withContext(Dispatchers.Main) {
                        showResultDialog(correctCount, totalQuestions, 0, "Perfect score! (Bonus claim failed)")
                     }
                }
            }
        } else {
            showResultDialog(correctCount, totalQuestions, 0, "Good effort! Keep learning.")
        }
    }

    private fun claimXP() {
        val email = sessionManager.fetchUserEmail() ?: return
        btnMarkCompleted.isEnabled = false
        btnMarkCompleted.text = "Claiming..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.instance.completeLesson(LessonCompleteRequest(email, videoId))
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LessonDetailActivity, "XP Earned!", Toast.LENGTH_SHORT).show()
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

    // --- UI Helpers ---

    private fun setOptionColor(rb: android.widget.RadioButton, isCorrect: Boolean) {
        val color = if (isCorrect) Color.GREEN else Color.RED
        rb.buttonTintList = ColorStateList.valueOf(color)
        rb.setTextColor(color)
    }

    private fun highlightCorrectAnswer(correctOption: String) {
        val correctRb = when (correctOption) {
            "A" -> rbOptionA
            "B" -> rbOptionB
            "C" -> rbOptionC
            "D" -> rbOptionD
            else -> null
        }
        if (correctRb != null) setOptionColor(correctRb, true)
    }

    private fun resetRadioButtonColors() {
        val color = tvQuestionText.textColors
        listOf(rbOptionA, rbOptionB, rbOptionC, rbOptionD).forEach {
            it.buttonTintList = null
            it.setTextColor(color)
        }
    }

    private fun enableRadioButtons() {
        for (i in 0 until radioGroupOptions.childCount) {
            radioGroupOptions.getChildAt(i).isEnabled = true
        }
    }

    private fun disableRadioButtons() {
        for (i in 0 until radioGroupOptions.childCount) {
            radioGroupOptions.getChildAt(i).isEnabled = false
        }
    }

    private fun showQuizLoading(show: Boolean) {
        progressQuiz.visibility = if (show) View.VISIBLE else View.GONE
        tvQuizError.visibility = View.GONE
        if (show) cardQuiz.visibility = View.GONE
    }

    private fun showQuizError(message: String, showFallbackButton: Boolean = false) {
        progressQuiz.visibility = View.GONE
        tvQuizError.text = message
        tvQuizError.visibility = View.VISIBLE
        cardQuiz.visibility = View.GONE
        
        if (showFallbackButton) {
            btnMarkCompleted.visibility = View.VISIBLE
        }
    }

    private fun showResultDialog(correct: Int, total: Int, xp: Int, message: String) {
        AlertDialog.Builder(this)
            .setTitle("Quiz Results")
            .setMessage("Score: $correct/$total\nXP Earned: $xp\n\n$message")
            .setPositiveButton("Close") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
