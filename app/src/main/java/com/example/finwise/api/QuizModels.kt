package com.example.finwise.api

// ============================================================================
// QUIZ MODELS (matching backend JSON structure)
// ============================================================================

/**
 * Represents a single quiz question with 4 multiple choice options.
 * Note: correct_option is NOT included (backend never sends it for security).
 */
data class Question(
    val id: Int,
    val question_text: String,
    val option_a: String,
    val option_b: String,
    val option_c: String,
    val option_d: String,
    val xp_value: Int = 10
)

/**
 * Represents a complete quiz with all its questions.
 * Retrieved via GET /api/learn/quiz/{videoId}
 */
data class Quiz(
    val id: Int,
    val title: String,
    val video_id: Int,
    val questions: List<Question>
)

/**
 * Request body for submitting quiz answers.
 * POST /api/learn/quiz/submit
 * 
 * @param email User's email address
 * @param quiz_id ID of the quiz being submitted
 * @param answers Map of question_id to answer choice ("A", "B", "C", or "D")
 *                Example: mapOf(1 to "A", 2 to "B", 3 to "C")
 */
data class QuizSubmission(
    val email: String,
    val quiz_id: Int,
    val answers: Map<String, String>  // Changed to Map<String, String> to match backend JSON format
)

/**
 * Response from quiz submission showing results and XP earned.
 * 
 * @param total_score Total points earned (10 XP per correct answer)
 * @param correct_count Number of questions answered correctly
 * @param total_questions Total number of questions in the quiz
 * @param xp_earned XP added to user's account (0 if already completed before)
 * @param message Success message or notification about XP farming prevention
 */
data class QuizResult(
    val total_score: Int,
    val correct_count: Int,
    val total_questions: Int,
    val xp_earned: Int,
    val message: String
)

data class CheckAnswerRequest(
    val email: String,
    val question_id: Int,
    val selected_option: String
)

data class CheckAnswerResponse(
    val is_correct: Boolean,
    val correct_option: String, // "A", "B", "C", "D"
    val xp_message: String // "+10 XP" or "Wrong answer"
)

data class ClaimBonusRequest(
    val email: String,
    val quiz_id: Int
)

data class BonusResponse(
    val success: Boolean,
    val message: String,
    val xp_bonus: Int
)
