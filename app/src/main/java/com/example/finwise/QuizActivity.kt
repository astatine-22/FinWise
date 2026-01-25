package com.example.finwise

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int // Index of correct option (0-3)
)

class QuizActivity : AppCompatActivity() {

    private lateinit var tvQuestionCounter: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var btnOption1: Button
    private lateinit var btnOption2: Button
    private lateinit var btnOption3: Button
    private lateinit var btnOption4: Button

    private var currentQuestionIndex = 0
    private var score = 0

    private val quizQuestions = listOf(
        QuizQuestion(
            "What is a Bull Market?",
            listOf(
                "A market where prices are falling",
                "A market where prices are rising",
                "A market with no change",
                "A market only for cattle"
            ),
            1
        ),
        QuizQuestion(
            "What does IPO stand for?",
            listOf(
                "International Public Offering",
                "Initial Private Offering",
                "Initial Public Offering",
                "Internal Price Offering"
            ),
            2
        ),
        QuizQuestion(
            "What is Diversification in investing?",
            listOf(
                "Investing all money in one stock",
                "Spreading investments across different assets",
                "Only buying government bonds",
                "Day trading frequently"
            ),
            1
        ),
        QuizQuestion(
            "What does P/E Ratio measure?",
            listOf(
                "Price to Earnings ratio",
                "Product to Export ratio",
                "Profit to Equity ratio",
                "Purchase to Expense ratio"
            ),
            0
        ),
        QuizQuestion(
            "What is a Stop-Loss order?",
            listOf(
                "An order to buy more stocks",
                "An order to sell when price reaches a certain level",
                "An order to hold stocks forever",
                "An order to stop trading"
            ),
            1
        ),
        QuizQuestion(
            "What is a Mutual Fund?",
            listOf(
                "A fund managed by robots",
                "A pool of money from many investors",
                "A government savings scheme",
                "A type of cryptocurrency"
            ),
            1
        ),
        QuizQuestion(
            "What is compound interest?",
            listOf(
                "Interest calculated only on principal",
                "Interest calculated on principal + previous interest",
                "Interest that never changes",
                "Interest paid monthly"
            ),
            1
        ),
        QuizQuestion(
            "What is a Bear Market?",
            listOf(
                "A market where prices are rising",
                "A market where prices are falling",
                "A market for selling bears",
                "A market with no volatility"
            ),
            1
        ),
        QuizQuestion(
            "What is Market Capitalization?",
            listOf(
                "The total value of a company's shares",
                "The capital city of markets",
                "The maximum price of a stock",
                "The minimum investment required"
            ),
            0
        ),
        QuizQuestion(
            "What is a Dividend?",
            listOf(
                "A tax on stocks",
                "A fee for trading",
                "A portion of company profits paid to shareholders",
                "A type of stock split"
            ),
            2
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        // Initialize views
        tvQuestionCounter = findViewById(R.id.tvQuestionCounter)
        tvQuestion = findViewById(R.id.tvQuestion)
        btnOption1 = findViewById(R.id.btnOption1)
        btnOption2 = findViewById(R.id.btnOption2)
        btnOption3 = findViewById(R.id.btnOption3)
        btnOption4 = findViewById(R.id.btnOption4)

        // Set click listeners
        btnOption1.setOnClickListener { checkAnswer(0) }
        btnOption2.setOnClickListener { checkAnswer(1) }
        btnOption3.setOnClickListener { checkAnswer(2) }
        btnOption4.setOnClickListener { checkAnswer(3) }

        // Load first question
        loadQuestion()
    }

    private fun loadQuestion() {
        if (currentQuestionIndex < quizQuestions.size) {
            val question = quizQuestions[currentQuestionIndex]
            
            // Update UI
            tvQuestionCounter.text = "Question ${currentQuestionIndex + 1}/${quizQuestions.size}"
            tvQuestion.text = question.question
            btnOption1.text = question.options[0]
            btnOption2.text = question.options[1]
            btnOption3.text = question.options[2]
            btnOption4.text = question.options[3]
        } else {
            showResults()
        }
    }

    private fun checkAnswer(selectedOption: Int) {
        val currentQuestion = quizQuestions[currentQuestionIndex]
        
        if (selectedOption == currentQuestion.correctAnswer) {
            score++
            Toast.makeText(this, "✅ Correct!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "❌ Wrong!", Toast.LENGTH_SHORT).show()
        }

        // Move to next question
        currentQuestionIndex++
        loadQuestion()
    }

    private fun showResults() {
        val percentage = (score * 100) / quizQuestions.size
        val resultMessage = """
            Quiz Completed! 🎉
            
            Your Score: $score/${quizQuestions.size}
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
