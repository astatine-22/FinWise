package com.example.finwise

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finwise.api.CheckAnswerRequest
import com.example.finwise.api.Question
import com.example.finwise.api.Quiz
import com.example.finwise.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuizActivity : AppCompatActivity() {

    private lateinit var tvQuizTitle: TextView
    private lateinit var tvQuestionCounter: TextView
    private lateinit var tvQuestionText: TextView
    private lateinit var btnOptionA: MaterialButton
    private lateinit var btnOptionB: MaterialButton
    private lateinit var btnOptionC: MaterialButton
    private lateinit var btnOptionD: MaterialButton
    private lateinit var btnNext: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var progressQuiz: ProgressBar
    
    private lateinit var sharedPreferences: SharedPreferences

    private var lessonId: Int = 0
    private var lessonTitle: String = ""
    private var quiz: Quiz? = null
    private var questions: List<Question> = emptyList()
    
    private var currentQuestionIndex = 0
    private var score = 0
    private var selectedOption: String? = null // "A", "B", "C", "D"

    companion object {
        private const val TAG = "QuizActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)
        
        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

        // Get data from intent
        lessonId = intent.getIntExtra("LESSON_ID", 0)
        lessonTitle = intent.getStringExtra("LESSON_TITLE") ?: "Quiz"
        
        initializeViews()
        setupClickListeners()
        
        // Fetch quiz from API
        fetchQuiz()
    }

    private fun initializeViews() {
        tvQuizTitle = findViewById(R.id.tvQuizTitle)
        tvQuestionCounter = findViewById(R.id.tvQuestionCounter)
        tvQuestionText = findViewById(R.id.tvQuestionText)
        btnOptionA = findViewById(R.id.btnOptionA)
        btnOptionB = findViewById(R.id.btnOptionB)
        btnOptionC = findViewById(R.id.btnOptionC)
        btnOptionD = findViewById(R.id.btnOptionD)
        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)
        progressQuiz = findViewById(R.id.progressQuiz)

        tvQuizTitle.text = lessonTitle
        // Initial state
        btnNext.isEnabled = false
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnOptionA.setOnClickListener { selectOption("A") }
        btnOptionB.setOnClickListener { selectOption("B") }
        btnOptionC.setOnClickListener { selectOption("C") }
        btnOptionD.setOnClickListener { selectOption("D") }

        btnNext.setOnClickListener {
            if (selectedOption == null) {
                Toast.makeText(this, "Please select an answer!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkAnswerAndProceed()
        }
    }

    private fun fetchQuiz() {
        // Show loading state
        tvQuestionText.text = "Loading quiz..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Fetching quiz for lesson ID: $lessonId")
                val fetchedQuiz = RetrofitClient.instance.getQuiz(lessonId)
                
                withContext(Dispatchers.Main) {
                    quiz = fetchedQuiz
                    questions = fetchedQuiz.questions
                    
                    if (questions.isNotEmpty()) {
                        Log.d(TAG, "Quiz fetched with ${questions.size} questions")
                        loadQuestion()
                    } else {
                        Log.e(TAG, "Quiz fetched but has no questions")
                        Toast.makeText(this@QuizActivity, "No questions found for this quiz.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching quiz: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@QuizActivity, "Failed to load quiz. Please try again.", Toast.LENGTH_SHORT).show()
                    tvQuestionText.text = "Error loading quiz."
                }
            }
        }
    }

    private fun selectOption(option: String) {
        selectedOption = option
        btnNext.isEnabled = true
        
        // Reset all button styles
        resetButtonStyles()
        
        // Highlight selected button
        val selectedButton = when (option) {
            "A" -> btnOptionA
            "B" -> btnOptionB
            "C" -> btnOptionC
            "D" -> btnOptionD
            else -> null
        }
        selectedButton?.setBackgroundColor(getColor(R.color.finwise_green))
        selectedButton?.setTextColor(getColor(android.R.color.white))
    }

    private fun resetButtonStyles() {
        listOf(btnOptionA, btnOptionB, btnOptionC, btnOptionD).forEach { button ->
            button.setBackgroundColor(getColor(android.R.color.transparent))
            button.setTextColor(getColor(R.color.finwise_green))
            button.strokeColor = getColorStateList(R.color.finwise_green)
        }
    }

    private fun loadQuestion() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            
            // Update progress
            val progress = ((currentQuestionIndex + 1) * 100) / questions.size
            progressQuiz.progress = progress
            
            // Update UI
            tvQuestionCounter.text = "Question ${currentQuestionIndex + 1} of ${questions.size}"
            tvQuestionText.text = question.question_text
            btnOptionA.text = "A. ${question.option_a}"
            btnOptionB.text = "B. ${question.option_b}"
            btnOptionC.text = "C. ${question.option_c}"
            btnOptionD.text = "D. ${question.option_d}"
            
            // Reset selection
            selectedOption = null
            btnNext.isEnabled = false
            resetButtonStyles()
            
            // Update button text
            btnNext.text = "Check Answer"
        } else {
            showResults()
        }
    }

    private fun checkAnswerAndProceed() {
        // Disable button to prevent double clicks
        btnNext.isEnabled = false
        
        val currentQuestion = questions[currentQuestionIndex]
        val email = sharedPreferences.getString("userEmail", "") ?: ""
        
        if (email.isEmpty()) {
            Toast.makeText(this, "User email not found. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = CheckAnswerRequest(
                    email = email,
                    question_id = currentQuestion.id,
                    selected_option = selectedOption!!
                )
                
                val response = RetrofitClient.instance.checkAnswer(request)
                
                withContext(Dispatchers.Main) {
                    if (response.is_correct) {
                        score++
                        Toast.makeText(this@QuizActivity, "✅ Correct!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@QuizActivity, "❌ Wrong! Answer: ${response.correct_option}", Toast.LENGTH_SHORT).show()
                    }
                    
                    // Delay slightly before moving to next question
                    tvQuestionText.postDelayed({
                        currentQuestionIndex++
                        loadQuestion()
                    }, 1000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking answer: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@QuizActivity, "Error checking answer.", Toast.LENGTH_SHORT).show()
                    btnNext.isEnabled = true
                }
            }
        }
    }

    private fun showResults() {
        val percentage = (score * 100) / questions.size
        val resultMessage = """
            Quiz Completed! 🎉
            
            Your Score: $score/${questions.size}
            Percentage: $percentage%
            
            ${getPerformanceMessage(percentage)}
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("Quiz Results")
            .setMessage(resultMessage)
            .setPositiveButton("Close") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun getPerformanceMessage(percentage: Int): String {
        return when {
            percentage >= 80 -> "Excellent! You're a finance expert! 🌟"
            percentage >= 60 -> "Good job! Keep learning! 📚"
            percentage >= 40 -> "Not bad! Practice more! 💪"
            else -> "Keep trying! Learning takes time! 🎯"
        }
    }
}
