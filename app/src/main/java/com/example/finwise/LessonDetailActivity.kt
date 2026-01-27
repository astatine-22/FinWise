package com.example.finwise

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.finwise.api.CheckAnswerRequest
import com.example.finwise.api.ClaimBonusRequest
import com.example.finwise.api.Question
import com.example.finwise.api.Quiz
import com.example.finwise.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LessonDetailActivity : AppCompatActivity() {

    private lateinit var webviewPlayer: WebView
    private lateinit var tvLessonTitle: TextView
    private lateinit var tvLessonDescription: TextView
    private lateinit var cardQuiz: CardView
    private lateinit var tvQuizHeader: TextView
    private lateinit var tvQuestionCounter: TextView
    private lateinit var tvQuestionText: TextView
    private lateinit var radioGroupOptions: RadioGroup
    private lateinit var rbOptionA: RadioButton
    private lateinit var rbOptionB: RadioButton
    private lateinit var rbOptionC: RadioButton
    private lateinit var rbOptionD: RadioButton
    private lateinit var btnNextConfirm: MaterialButton
    private lateinit var progressQuiz: ProgressBar
    private lateinit var tvQuizError: TextView
    private lateinit var btnMarkCompleted: Button
    private lateinit var sharedPreferences: SharedPreferences

    private var lessonId: Int = 0
    private var lessonTitle: String = ""
    private var lessonSubtitle: String = ""
    private var lessonXp: String = ""
    private var lessonVideoUrl: String = ""
    private var lessonYoutubeId: String = ""

    private var quiz: Quiz? = null
    private var currentQuestionIndex: Int = 0
    private var userAnswers: MutableMap<String, String> = mutableMapOf()
    private var totalXpEarned: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_detail)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

        // Get data from intent
        lessonId = intent.getIntExtra("LESSON_ID", 0)
        lessonTitle = intent.getStringExtra("LESSON_TITLE") ?: ""
        lessonSubtitle = intent.getStringExtra("LESSON_SUBTITLE") ?: ""
        lessonXp = intent.getStringExtra("LESSON_XP") ?: ""
        lessonVideoUrl = intent.getStringExtra("LESSON_VIDEO_URL") ?: ""
        lessonYoutubeId = intent.getStringExtra("LESSON_YOUTUBE_ID") ?: ""

        initializeViews()
        setupWebView()
        displayLessonInfo()
        fetchQuiz()
    }

    private fun initializeViews() {
        webviewPlayer = findViewById(R.id.webview_player)
        tvLessonTitle = findViewById(R.id.tvLessonTitle)
        tvLessonDescription = findViewById(R.id.tvLessonDescription)
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
        btnMarkCompleted = findViewById(R.id.btnMarkCompleted)

        btnNextConfirm.setOnClickListener {
            handleCheckAnswer()
        }

        btnMarkCompleted.setOnClickListener {
            claimXP(100)
        }
    }

    private fun setupWebView() {
        // Enable JavaScript (required for YouTube)
        webviewPlayer.settings.javaScriptEnabled = true
        
        // Enable DOM storage (required for YouTube player)
        webviewPlayer.settings.domStorageEnabled = true
        
        // Enable media playback without user gesture
        webviewPlayer.settings.mediaPlaybackRequiresUserGesture = false
        
        // Enable file access for web content
        webviewPlayer.settings.allowFileAccess = true
        webviewPlayer.settings.allowContentAccess = true
        
        // Enable zoom controls
        webviewPlayer.settings.builtInZoomControls = true
        webviewPlayer.settings.displayZoomControls = false
        
        // Load images automatically
        webviewPlayer.settings.loadsImagesAutomatically = true
        
        // Enable viewport and load with overview mode for proper scaling
        webviewPlayer.settings.useWideViewPort = true
        webviewPlayer.settings.loadWithOverviewMode = true
        
        // Set WebViewClient to handle page loading
        webviewPlayer.webViewClient = WebViewClient()

        // Get the normal YouTube URL from database (no conversion needed)
        val youtubeUrl = when {
            lessonVideoUrl.isNotEmpty() -> lessonVideoUrl
            lessonYoutubeId.isNotEmpty() -> "https://www.youtube.com/watch?v=$lessonYoutubeId"
            else -> ""
        }

        // Load the normal YouTube URL directly
        if (youtubeUrl.isNotEmpty()) {
            webviewPlayer.loadUrl(youtubeUrl)
        }
    }

    private fun displayLessonInfo() {
        tvLessonTitle.text = lessonTitle
        tvLessonDescription.text = lessonSubtitle
    }

    private fun fetchQuiz() {
        progressQuiz.visibility = View.VISIBLE
        tvQuizError.visibility = View.GONE
        cardQuiz.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fetchedQuiz = RetrofitClient.instance.getQuiz(lessonId)
                quiz = fetchedQuiz

                withContext(Dispatchers.Main) {
                    progressQuiz.visibility = View.GONE
                    if (fetchedQuiz.questions.isNotEmpty()) {
                        cardQuiz.visibility = View.VISIBLE
                        displayQuestion(0)
                    } else {
                        tvQuizError.visibility = View.VISIBLE
                        tvQuizError.text = "No quiz available for this lesson"
                        btnMarkCompleted.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressQuiz.visibility = View.GONE
                    tvQuizError.visibility = View.VISIBLE
                    tvQuizError.text = "Failed to load quiz"
                    btnMarkCompleted.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun displayQuestion(index: Int) {
        val questions = quiz?.questions ?: return
        if (index >= questions.size) {
            handleQuizCompletion()
            return
        }

        val question = questions[index]
        currentQuestionIndex = index

        tvQuestionCounter.text = "Question ${index + 1} of ${questions.size}"
        tvQuestionText.text = question.question_text
        rbOptionA.text = question.option_a
        rbOptionB.text = question.option_b
        rbOptionC.text = question.option_c
        rbOptionD.text = question.option_d

        // Clear previous selection
        radioGroupOptions.clearCheck()

        // Update button text
        btnNextConfirm.text = if (index == questions.size - 1) "Submit" else "Next Question"
    }

    private fun handleCheckAnswer() {
        val selectedId = radioGroupOptions.checkedRadioButtonId

        if (selectedId == -1) {
            Toast.makeText(this, "Please select an answer!", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedOption = when (selectedId) {
            R.id.rbOptionA -> "A"
            R.id.rbOptionB -> "B"
            R.id.rbOptionC -> "C"
            R.id.rbOptionD -> "D"
            else -> ""
        }

        val currentQuestion = quiz?.questions?.get(currentQuestionIndex) ?: return
        val questionId = currentQuestion.id

        // Store the user's answer
        userAnswers[questionId.toString()] = selectedOption

        // Check answer with backend
        val email = sharedPreferences.getString("userEmail", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = CheckAnswerRequest(email, questionId, selectedOption)
                val response = RetrofitClient.instance.checkAnswer(request)

                withContext(Dispatchers.Main) {
                    if (response.is_correct) {
                        Toast.makeText(
                            this@LessonDetailActivity,
                            "Correct! ${response.xp_message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        totalXpEarned += currentQuestion.xp_value
                    } else {
                        Toast.makeText(
                            this@LessonDetailActivity,
                            "Wrong! Correct answer: ${response.correct_option}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    // Move to next question after a short delay
                    webviewPlayer.postDelayed({
                        displayQuestion(currentQuestionIndex + 1)
                    }, 1000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@LessonDetailActivity,
                        "Error checking answer",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Move to next question anyway
                    displayQuestion(currentQuestionIndex + 1)
                }
            }
        }
    }

    private fun handleQuizCompletion() {
        cardQuiz.visibility = View.GONE
        
        val email = sharedPreferences.getString("userEmail", "") ?: ""
        val quizId = quiz?.id ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = ClaimBonusRequest(email, quizId)
                val response = RetrofitClient.instance.claimBonus(request)

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        totalXpEarned += response.xp_bonus
                        Toast.makeText(
                            this@LessonDetailActivity,
                            "${response.message}\nTotal XP Earned: $totalXpEarned",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@LessonDetailActivity,
                            response.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@LessonDetailActivity,
                        "Quiz completed! Total XP: $totalXpEarned",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun claimXP(xp: Int) {
        Toast.makeText(this, "Claimed $xp XP!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
