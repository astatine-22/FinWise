package com.example.finwise

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
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
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LessonDetailActivity : AppCompatActivity() {

    private lateinit var youtubePlayerView: YouTubePlayerView
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
    private var isAnswerChecked = false
    private var correctCount = 0

    // Content State
    private var isVideoCompleted = false
    private var videoId: Int = 0
    private var quizId: Int = 0
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_lesson_detail)

            // Initialize SessionManager
            sessionManager = SessionManager.getInstance(this)

            // Initialize views
            youtubePlayerView = findViewById(R.id.youtube_player_view)
            lifecycle.addObserver(youtubePlayerView)

            tvLessonTitle = findViewById(R.id.tvLessonTitle)
            tvLessonDescription = findViewById(R.id.tvLessonDescription)
            btnMarkCompleted = findViewById(R.id.btnMarkCompleted)

            // Initialize quiz views
            layoutQuizSection = findViewById(R.id.cardQuiz)
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
            android.util.Log.e("LessonDetail", "Video URL from Intent: $videoUrl")

            val title = intent.getStringExtra("TITLE") ?: "Lesson"
            val subtitle = intent.getStringExtra("SUBTITLE") ?: ""
            videoId = intent.getIntExtra("VIDEO_ID", 0)

            // Set title and description
            tvLessonTitle.text = title
            tvLessonDescription.text = subtitle

            // Setup YouTube Player
            val videoIdString = extractYouTubeId(videoUrl)
            if (videoIdString.isNotEmpty()) {
                setupYouTubePlayer(videoIdString)
            } else {
                 Toast.makeText(this, "Error loading video: Invalid URL", Toast.LENGTH_SHORT).show()
            }

            // Fetch quiz
            fetchQuiz()

            // Listeners
            btnMarkCompleted.setOnClickListener { claimXP() }

            btnNextConfirm.setOnClickListener {
                if (isAnswerChecked) {
                    handleNextClick()
                } else {
                    handleCheckAnswer()
                }
            }

            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error opening lesson: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
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
                        selectedRb.buttonTintList = ColorStateList.valueOf(Color.GREEN)
                        selectedRb.setTextColor(Color.GREEN)
                        Toast.makeText(this@LessonDetailActivity, response.xp_message, Toast.LENGTH_SHORT).show()
                    } else {
                        selectedRb.buttonTintList = ColorStateList.valueOf(Color.RED)
                        selectedRb.setTextColor(Color.RED)
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

    private fun highlightCorrectAnswer(correctOption: String) {
        val correctRb = when (correctOption) {
            "A" -> rbOptionA
            "B" -> rbOptionB
            "C" -> rbOptionC
            "D" -> rbOptionD
            else -> null
        }
        correctRb?.buttonTintList = ColorStateList.valueOf(Color.GREEN)
        correctRb?.setTextColor(Color.GREEN)
    }

    private fun disableRadioButtons() {
        for (i in 0 until radioGroupOptions.childCount) {
            radioGroupOptions.getChildAt(i).isEnabled = false
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
    }

    private fun enableRadioButtons() {
        for (i in 0 until radioGroupOptions.childCount) {
            radioGroupOptions.getChildAt(i).isEnabled = true
        }
    }

    private fun resetRadioButtonColors() {
        rbOptionA.buttonTintList = null
        rbOptionB.buttonTintList = null
        rbOptionC.buttonTintList = null
        rbOptionD.buttonTintList = null

        val color = tvQuestionText.textColors
        rbOptionA.setTextColor(color)
        rbOptionB.setTextColor(color)
        rbOptionC.setTextColor(color)
        rbOptionD.setTextColor(color)
    }

    private fun handleQuizCompletion() {
        val totalQuestions = quizQuestions.size

        if (correctCount == totalQuestions) {
            val userEmail = sessionManager.fetchUserEmail() ?: return
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
                        showResultDialog(correctCount, totalQuestions, 0, "Perfect score, but bonus claim failed.")
                    }
                }
            }
        } else {
            showResultDialog(correctCount, totalQuestions, 0, "Good effort!")
        }
    }

    private fun showResultDialog(
        correctCount: Int,
        totalQuestions: Int,
        xpEarned: Int,
        message: String
    ) {
        val dialogMessage = "You scored $correctCount/$totalQuestions\nXP Earned: $xpEarned\n\n$message"
        AlertDialog.Builder(this)
            .setTitle("🎉 Quiz Results")
            .setMessage(dialogMessage)
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun extractYouTubeId(url: String): String {
        return if (url.contains("v=")) {
            url.substringAfter("v=").substringBefore("&")
        } else if (url.contains("/embed/")) {
            url.substringAfter("/embed/").substringBefore("?")
        } else if (url.contains("youtu.be/")) {
            url.substringAfter("youtu.be/").substringBefore("?")
        } else {
            // Assume it's a raw ID if it doesn't match URL patterns
            url
        }
    }

    private fun setupYouTubePlayer(videoId: String) {
        youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                if (videoId.isNotEmpty()) {
                    youTubePlayer.loadVideo(videoId, 0f)
                }
            }

            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                if (state == PlayerConstants.PlayerState.ENDED) {
                    if (!isVideoCompleted) {
                        isVideoCompleted = true
                        claimXP()
                        Toast.makeText(this@LessonDetailActivity, "🎉 Lecture Complete! +100 XP", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun claimXP() {
        val sharedPref = getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        val email = sharedPref.getString("LOGGED_IN_EMAIL", null)
        if (email == null) return

        btnMarkCompleted.isEnabled = false
        btnMarkCompleted.text = "Claiming XP..."

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
        correctCount = 0

        cardQuiz.visibility = View.VISIBLE
        btnMarkCompleted.visibility = View.GONE

        if (quizQuestions.isNotEmpty()) {
            showQuestion(0)
        } else {
            showQuizError("No questions found", showFallbackButton = true)
        }
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
