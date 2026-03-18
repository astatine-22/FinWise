package com.example.finwise

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finwise.api.LessonCompleteRequest
import com.example.finwise.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LocalQuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int // 0 for A, 1 for B, 2 for C, 3 for D
)

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
    private var questions: List<LocalQuizQuestion> = emptyList()
    
    private var currentQuestionIndex = 0
    private var score = 0
    private var selectedOptionIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)
        
        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

        // Get data from intent
        lessonId = intent.getIntExtra("LESSON_ID", 0)
        lessonTitle = intent.getStringExtra("LESSON_TITLE") ?: "Quiz"
        
        initializeViews()
        setupClickListeners()
        loadLocalQuizData()
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
        btnNext.isEnabled = false
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnOptionA.setOnClickListener { selectOption(0) }
        btnOptionB.setOnClickListener { selectOption(1) }
        btnOptionC.setOnClickListener { selectOption(2) }
        btnOptionD.setOnClickListener { selectOption(3) }

        btnNext.setOnClickListener {
            if (selectedOptionIndex == -1) {
                Toast.makeText(this, "Please select an answer!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkAnswerAndProceed()
        }
    }

    private fun loadLocalQuizData() {
        questions = when {
            // ----- Investing Basics -----
            lessonTitle.contains("Stock Market", ignoreCase = true) -> listOf(
                LocalQuizQuestion("What does IPO stand for?",
                    listOf("Initial Public Offering", "Indian Public Office", "Internal Profit Order", "Initial Private Owner"), 0),
                LocalQuizQuestion("What do you actually own when you buy a share?",
                    listOf("A digital token", "A partial ownership in the company", "A debt paper", "Nothing, it's just gambling"), 1),
                LocalQuizQuestion("Which is a major Stock Exchange in India?",
                    listOf("NYSE", "NSE", "FOREX", "NASDAQ"), 1),
                LocalQuizQuestion("Who regulates the Stock Market in India?",
                    listOf("RBI", "SEBI", "SBI", "Government of India"), 1),
                LocalQuizQuestion("What is a 'Bull Market'?",
                    listOf("When prices are falling", "When prices are rising", "When the market is closed", "A market for selling cattle"), 1)
            )

            lessonTitle.contains("Buying Your First Stock", ignoreCase = true) -> listOf(
                LocalQuizQuestion("What is a Demat account used for?",
                    listOf("Storing physical share certificates", "Holding shares in electronic form", "Making fixed deposits", "Transferring money internationally"), 1),
                LocalQuizQuestion("A market order means:",
                    listOf("Buy only if the price falls", "Buy at the current market price immediately", "Buy tomorrow", "Buy in bulk only"), 1),
                LocalQuizQuestion("What is a 'ticker symbol'?",
                    listOf("A daily stock newsletter", "A short code representing a company's stock", "A noise the market makes", "A broker's commission code"), 1),
                LocalQuizQuestion("A limit order lets you:",
                    listOf("Buy only at a price you specify", "Buy at any available price", "Trade on weekends", "Buy without a broker"), 0),
                LocalQuizQuestion("Which of these is the correct symbol for Tata Consultancy Services on NSE?",
                    listOf("TATA.NS", "TCS.NS", "TCS.IN", "TATACS"), 1)
            )

            lessonTitle.contains("Investing for Beginners", ignoreCase = true) -> listOf(
                LocalQuizQuestion("What is the key difference between saving and investing?",
                    listOf("There is no difference", "Investing grows wealth over time; saving just preserves it", "Saving always earns more", "Investing is risk-free"), 1),
                LocalQuizQuestion("'Risk vs Reward' in investing means:",
                    listOf("Higher risk always means higher guaranteed returns", "Higher potential returns usually come with higher risk", "Low-risk investments never grow", "Reward is fixed regardless of risk"), 1),
                LocalQuizQuestion("Long-term investing typically means holding for:",
                    listOf("1 day", "1 week", "Several years or more", "1 month"), 2),
                LocalQuizQuestion("Which mindset is best for a beginner investor?",
                    listOf("Try to get rich overnight", "Panic sell when prices fall", "Stay patient and think long-term", "Invest all savings in one stock"), 2),
                LocalQuizQuestion("Diversification means:",
                    listOf("Investing all money in one asset", "Spreading investments across different assets to reduce risk", "Only buying government bonds", "Selling all assets frequently"), 1)
            )

            // ----- Mutual Funds -----
            lessonTitle.contains("Mutual Fund", ignoreCase = true) ||
            lessonTitle.contains("SIP", ignoreCase = true) ||
            lessonTitle.contains("Index Fund", ignoreCase = true) -> when {
                lessonTitle.contains("SIP", ignoreCase = true) -> listOf(
                    LocalQuizQuestion("What does SIP stand for?",
                        listOf("Systematic Investment Plan", "Stock Investment Policy", "Simple Interest Payment", "Savings Income Plan"), 0),
                    LocalQuizQuestion("What is Rupee Cost Averaging?",
                        listOf("Buying more units when price is high", "Buying units at different prices over time to average the cost", "Always buying at the same fixed price", "A banking term for exchange rates"), 1),
                    LocalQuizQuestion("Which is generally better for beginners — SIP or Lumpsum?",
                        listOf("Lumpsum always", "SIP, because it reduces timing risk", "Both are equally risky", "Neither works for beginners"), 1),
                    LocalQuizQuestion("When is Lumpsum investing preferred?",
                        listOf("When markets are at all-time highs", "When you have a large amount and markets are at a low", "Every month without exception", "Never"), 1),
                    LocalQuizQuestion("SIP helps beginners by:",
                        listOf("Requiring a large upfront deposit", "Letting them invest small amounts regularly", "Guaranteeing returns", "Avoiding all market risk"), 1)
                )
                lessonTitle.contains("Index Fund", ignoreCase = true) -> listOf(
                    LocalQuizQuestion("What is an Index Fund?",
                        listOf("A fund managed by experts to beat the market", "A fund that tracks a market index like Nifty 50", "A fixed deposit scheme", "A government bond"), 1),
                    LocalQuizQuestion("What is an expense ratio?",
                        listOf("The profit a fund makes", "The fee charged by a fund to manage your money", "The tax on returns", "The minimum investment amount"), 1),
                    LocalQuizQuestion("Active management means:",
                        listOf("A fund that always follows an index", "Fund managers actively pick stocks trying to beat the market", "Investing only in government securities", "A strategy for day trading"), 1),
                    LocalQuizQuestion("What is a major advantage of Index Funds over active funds?",
                        listOf("They always outperform the market", "They have lower fees and often match the market returns", "They guarantee no losses", "They don't need a broker"), 1),
                    LocalQuizQuestion("Nifty 50 represents:",
                        listOf("The top 50 bonds in India", "The top 50 companies listed on NSE", "India's national budget allocation", "Top 50 banks in India"), 1)
                )
                else -> listOf( // General Mutual Funds
                    LocalQuizQuestion("What is a Mutual Fund?",
                        listOf("A savings account", "A pooled investment managed by a fund manager", "A government scheme", "A type of loan"), 1),
                    LocalQuizQuestion("Diversification in mutual funds means:",
                        listOf("All money in one stock", "Investing across many assets to reduce risk", "Only investing in bonds", "Only investing in gold"), 1),
                    LocalQuizQuestion("What type of mutual fund invests mainly in stocks?",
                        listOf("Debt Fund", "Equity Fund", "Liquid Fund", "Hybrid Fund"), 1),
                    LocalQuizQuestion("Who manages a mutual fund?",
                        listOf("The government", "The investor directly", "A professional fund manager", "A bank teller"), 2),
                    LocalQuizQuestion("A Hybrid Fund invests in:",
                        listOf("Only stocks", "Only bonds", "A mix of stocks and bonds", "Only gold"), 2)
                )
            }

            // ----- Wealth Building -----
            lessonTitle.contains("Compounding", ignoreCase = true) -> listOf(
                LocalQuizQuestion("What is compound interest?",
                    listOf("Interest on the principal only", "Interest earned on both principal and previously earned interest", "A type of bank loan", "Monthly bank charges"), 1),
                LocalQuizQuestion("Who called compound interest the '8th wonder of the world'?",
                    listOf("Warren Buffett", "Albert Einstein", "Ratan Tata", "Isaac Newton"), 1),
                LocalQuizQuestion("If you invest ₹10,000 at 10% compounded annually, after 2 years you have approximately:",
                    listOf("₹12,000", "₹12,100", "₹11,000", "₹10,200"), 1),
                LocalQuizQuestion("Why is starting early so important in compounding?",
                    listOf("You need less money to invest when young", "More years means more compounding cycles and exponential growth", "Banks give better rates to young investors", "There is no real advantage to starting early"), 1),
                LocalQuizQuestion("What does 'exponential growth' mean in investing?",
                    listOf("Linear growth year by year", "Growth that accelerates over time as returns build on returns", "Growth that slows down over time", "Only applies to crypto"), 1)
            )

            // ----- Personal Finance -----
            lessonTitle.contains("50/30/20", ignoreCase = true) ||
            lessonTitle.contains("Budget Rule", ignoreCase = true) -> listOf(
                LocalQuizQuestion("In the 50/30/20 rule, 50% goes to:",
                    listOf("Wants", "Savings", "Needs", "Investments"), 2),
                LocalQuizQuestion("Which of these is a 'Want' in the 50/30/20 rule?",
                    listOf("Rent", "Groceries", "Netflix subscription", "Electricity bill"), 2),
                LocalQuizQuestion("The 20% in the 50/30/20 rule is for:",
                    listOf("Entertainment", "Food", "Savings and investments", "Clothes"), 2),
                LocalQuizQuestion("Which of these is a 'Need'?",
                    listOf("Going to a concert", "Buying new sneakers", "Paying rent", "Eating at a restaurant every day"), 2),
                LocalQuizQuestion("Why is the 50/30/20 rule popular?",
                    listOf("It guarantees wealth", "It is simple to follow for budgeting", "It is a government mandate", "It only works for high earners"), 1)
            )

            lessonTitle.contains("Emergency Fund", ignoreCase = true) -> listOf(
                LocalQuizQuestion("How many months of expenses should an emergency fund cover?",
                    listOf("1 month", "3 months", "6 months", "12 months"), 2),
                LocalQuizQuestion("Where should you park your emergency fund?",
                    listOf("In high-risk stocks", "In a liquid fund or savings account", "In real estate", "In cryptocurrencies"), 1),
                LocalQuizQuestion("What is the main purpose of an emergency fund?",
                    listOf("To earn high returns", "To pay for vacations", "To handle unexpected expenses without debt", "To invest in stocks"), 2),
                LocalQuizQuestion("A liquid fund is ideal for emergency savings because:",
                    listOf("It has the highest returns", "Money can be withdrawn quickly with low risk", "It is guaranteed by the government", "It never loses value"), 1),
                LocalQuizQuestion("Without an emergency fund, unexpected costs often lead to:",
                    listOf("Higher investment returns", "Taking on debt", "Better savings habits", "Lower taxes"), 1)
            )

            lessonTitle.contains("Income Tax", ignoreCase = true) ||
            lessonTitle.contains("80C", ignoreCase = true) -> listOf(
                LocalQuizQuestion("What is Section 80C in Indian Income Tax?",
                    listOf("A provision for home loans", "A section allowing deductions up to ₹1.5 lakh on specified investments", "A capital gains tax rule", "A TDS refund process"), 1),
                LocalQuizQuestion("TDS stands for:",
                    listOf("Tax Deduction Scheme", "Tax Deducted at Source", "Total Deposit Sum", "Trade Dividend Settlement"), 1),
                LocalQuizQuestion("Under the New Tax Regime, which of the following is TRUE?",
                    listOf("You can still claim 80C deductions", "Most deductions and exemptions are not available", "Tax is calculated on gross income without any slab", "It applies only to businesses"), 1),
                LocalQuizQuestion("ELSS is a tax-saving investment category. What does ELSS mean?",
                    listOf("Equity Linked Savings Scheme", "Emergency Loan Support Scheme", "Employer Liability Social Security", "Equal Loss Sharing System"), 0),
                LocalQuizQuestion("PPF (Public Provident Fund) qualifies for deduction under:",
                    listOf("Section 80D", "Section 80C", "Section 10A", "Section 24B"), 1)
            )

            // ----- Crypto -----
            lessonTitle.contains("Bitcoin", ignoreCase = true) -> listOf(
                LocalQuizQuestion("Who created Bitcoin?",
                    listOf("Elon Musk", "Satoshi Nakamoto", "Vitalik Buterin", "Bill Gates"), 1),
                LocalQuizQuestion("What does 'decentralized' mean in the context of Bitcoin?",
                    listOf("Controlled by one bank", "Managed by the government", "No central authority controls it", "Only accessible in one country"), 2),
                LocalQuizQuestion("Bitcoin is often compared to which traditional asset?",
                    listOf("Silver", "Oil", "Digital Gold", "Real estate"), 2),
                LocalQuizQuestion("What is the maximum supply of Bitcoin?",
                    listOf("Unlimited", "21 million", "100 million", "1 billion"), 1),
                LocalQuizQuestion("Blockchain is the technology that powers Bitcoin. It is:",
                    listOf("A centralized database", "A distributed public ledger", "A private bank network", "A type of wallet"), 1)
            )

            lessonTitle.contains("Blockchain", ignoreCase = true) -> listOf(
                LocalQuizQuestion("What is a blockchain?",
                    listOf("A type of cryptocurrency", "A chain of blocks storing transaction records in a distributed way", "A private banking system", "A stock exchange"), 1),
                LocalQuizQuestion("Why is a blockchain considered 'immutable'?",
                    listOf("Records can be changed by any user", "Once data is added, it is extremely difficult to alter", "Only the creator can modify records", "It stores data in one central server"), 1),
                LocalQuizQuestion("What is a 'block' in a blockchain?",
                    listOf("A financial institution", "A set of transaction records grouped together", "A type of encryption key", "A user account"), 1),
                LocalQuizQuestion("Which of these is a real-world use case for blockchain beyond crypto?",
                    listOf("Weather forecasting", "Social media algorithms", "Supply chain tracking", "TV broadcasting"), 2),
                LocalQuizQuestion("Consensus in blockchain means:",
                    listOf("One person approves all transactions", "All nodes in the network agree on the validity of data", "The government validates transactions", "Banks sign off on every block"), 1)
            )

            lessonTitle.contains("Crypto vs Stock", ignoreCase = true) -> listOf(
                LocalQuizQuestion("Compared to stocks, crypto markets are:",
                    listOf("Open only 9 AM – 3 PM weekdays", "Open 24/7, 365 days a year", "Open only in the USA", "Regulated the same way"), 1),
                LocalQuizQuestion("When you buy a stock, you own:",
                    listOf("Nothing", "A unit of cryptocurrency", "A fraction of a company", "A loan to the company"), 2),
                LocalQuizQuestion("Crypto is generally considered:",
                    listOf("Less volatile than stocks", "More volatile than stocks", "Equally volatile as stocks", "Not volatile at all"), 1),
                LocalQuizQuestion("Which of these is a key risk of cryptocurrency?",
                    listOf("Too much government regulation globally", "Extreme price swings (volatility)", "Guaranteed loss", "It can only be bought in large amounts"), 1),
                LocalQuizQuestion("Indian stocks are traded on NSE/BSE during:",
                    listOf("9:15 AM – 3:30 PM IST on weekdays", "24 hours a day", "Only on Saturdays", "8 AM – 8 PM IST"), 0)
            )

            lessonTitle.contains("Buy Crypto", ignoreCase = true) ||
            lessonTitle.contains("Crypto Safe", ignoreCase = true) -> listOf(
                LocalQuizQuestion("A 'hot wallet' is:",
                    listOf("A wallet stored offline", "A wallet connected to the internet", "A physical hardware device", "A government-issued wallet"), 1),
                LocalQuizQuestion("A 'cold wallet' is:",
                    listOf("A wallet connected to the internet", "An offline wallet for safer storage", "A bank savings account", "A crypto exchange account"), 1),
                LocalQuizQuestion("What is a phishing scam in crypto?",
                    listOf("Selling fake fish online", "Tricking users into giving up their private keys or passwords", "Buying crypto at a high price", "A type of blockchain attack"), 1),
                LocalQuizQuestion("Which of these is considered a safe centralized exchange in India?",
                    listOf("WazirX or CoinDCX", "WhatsApp groups", "Random Telegram channels", "Email attachments"), 0),
                LocalQuizQuestion("Why should you never share your seed phrase?",
                    listOf("It is only used once", "Anyone with your seed phrase can access and steal all your crypto", "It expires after 24 hours", "It is not important"), 1)
            )

            // ----- Fallback generic -----
            else -> listOf(
                LocalQuizQuestion("What is financial literacy?",
                    listOf("The ability to read financial news", "Understanding how to manage money, budget, invest, and plan", "Knowing how to use a bank ATM", "Passing a government finance exam"), 1),
                LocalQuizQuestion("Why is investing important?",
                    listOf("To spend money faster", "To grow wealth and beat inflation over time", "It is not important", "To win lottery prizes"), 1),
                LocalQuizQuestion("What is inflation?",
                    listOf("The rise in value of money over time", "The general increase in prices reducing purchasing power", "A type of investment", "A government tax"), 1),
                LocalQuizQuestion("Which is the safest short-term investment option in India?",
                    listOf("Penny stocks", "Fixed Deposit or Liquid Funds", "Cryptocurrency", "Futures and Options"), 1),
                LocalQuizQuestion("What is the best first step toward financial freedom?",
                    listOf("Take a large loan and invest it", "Spend everything now and save later", "Build a budget and start saving consistently", "Avoid all investments"), 2)
            )
        }

        if (questions.isNotEmpty()) {
            loadQuestion()
        } else {
            Toast.makeText(this, "No quiz available for this lesson", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun selectOption(index: Int) {
        selectedOptionIndex = index
        btnNext.isEnabled = true
        
        resetButtonStyles()
        
        val selectedButton = when (index) {
            0 -> btnOptionA
            1 -> btnOptionB
            2 -> btnOptionC
            3 -> btnOptionD
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
            
            val progress = ((currentQuestionIndex + 1) * 100) / questions.size
            progressQuiz.progress = progress
            
            tvQuestionCounter.text = "Question ${currentQuestionIndex + 1} of ${questions.size}"
            tvQuestionText.text = question.question
            btnOptionA.text = "A. ${question.options[0]}"
            btnOptionB.text = "B. ${question.options[1]}"
            btnOptionC.text = "C. ${question.options[2]}"
            btnOptionD.text = "D. ${question.options[3]}"
            
            selectedOptionIndex = -1
            btnNext.isEnabled = false
            resetButtonStyles()
            
            btnNext.text = "Check Answer"
        } else {
            showResults()
        }
    }

    private fun checkAnswerAndProceed() {
        btnNext.isEnabled = false
        
        val currentQuestion = questions[currentQuestionIndex]
        
        if (selectedOptionIndex == currentQuestion.correctAnswerIndex) {
            score++
            Toast.makeText(this, "✅ Correct!", Toast.LENGTH_SHORT).show()
        } else {
            val correctOptionChar = when (currentQuestion.correctAnswerIndex) {
                0 -> "A"; 1 -> "B"; 2 -> "C"; 3 -> "D"; else -> "?"
            }
            Toast.makeText(this, "❌ Wrong! Correct: $correctOptionChar", Toast.LENGTH_SHORT).show()
        }

        tvQuestionText.postDelayed({
            currentQuestionIndex++
            loadQuestion()
        }, 1000)
    }

    private fun showResults() {
        val percentage = (score * 100) / questions.size
        val xpEarned = if (percentage >= 60) 100 else 10

        val resultMessage = """
            Quiz Completed! 🎉
            
            Your Score: $score/${questions.size}
            Percentage: $percentage%
            XP Earned: $xpEarned
            
            ${getPerformanceMessage(percentage)}
        """.trimIndent()

        // Sync XP with backend — uses lessonId so backend can de-duplicate
        if (xpEarned > 0) {
            syncXP()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Quiz Results")
            .setMessage(resultMessage)
            .setPositiveButton("Close") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    /**
     * Syncs XP to the backend by calling POST /api/learn/complete with the lessonId.
     * The backend uses UserVideoProgress to ensure XP is awarded only once per lesson.
     */
    private fun syncXP() {
        val sharedPrefFinWise = getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        val email = sharedPrefFinWise.getString("LOGGED_IN_EMAIL", null) ?: run {
            // Fallback: try MyAppPrefs
            val altPrefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            altPrefs.getString("userEmail", null)
        } ?: return

        if (lessonId <= 0) {
            Log.w("QuizActivity", "lessonId is 0 — cannot sync XP without a valid lesson ID")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.completeLesson(
                    LessonCompleteRequest(email = email, video_id = lessonId)
                )
                Log.d("QuizActivity", "XP sync response: ${response.message}")
            } catch (e: Exception) {
                Log.e("QuizActivity", "XP sync failed: ${e.message}")
            }
        }
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
