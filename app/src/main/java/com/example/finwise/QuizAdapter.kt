package com.example.finwise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.Question

class QuizAdapter(private val questions: List<Question>) :
    RecyclerView.Adapter<QuizAdapter.QuestionViewHolder>() {

    // Store user's selected answers: Map<question_id, selected_option>
    private val selectedAnswers = mutableMapOf<Int, String>()

    inner class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuestionNumber: TextView = itemView.findViewById(R.id.tvQuestionNumber)
        val tvQuestionText: TextView = itemView.findViewById(R.id.tvQuestionText)
        val radioGroupOptions: RadioGroup = itemView.findViewById(R.id.radioGroupOptions)
        val radioOptionA: RadioButton = itemView.findViewById(R.id.radioOptionA)
        val radioOptionB: RadioButton = itemView.findViewById(R.id.radioOptionB)
        val radioOptionC: RadioButton = itemView.findViewById(R.id.radioOptionC)
        val radioOptionD: RadioButton = itemView.findViewById(R.id.radioOptionD)
        val tvXpValue: TextView = itemView.findViewById(R.id.tvXpValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quiz_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val question = questions[position]

        // Set question number and text
        holder.tvQuestionNumber.text = "Question ${position + 1}"
        holder.tvQuestionText.text = question.question_text

        // Set options
        holder.radioOptionA.text = "A. ${question.option_a}"
        holder.radioOptionB.text = "B. ${question.option_b}"
        holder.radioOptionC.text = "C. ${question.option_c}"
        holder.radioOptionD.text = "D. ${question.option_d}"

        // Set XP value
        holder.tvXpValue.text = "⭐ ${question.xp_value} XP"

        // Clear previous selection
        holder.radioGroupOptions.clearCheck()

        // Restore previous selection if exists
        selectedAnswers[question.id]?.let { selectedOption ->
            when (selectedOption) {
                "A" -> holder.radioOptionA.isChecked = true
                "B" -> holder.radioOptionB.isChecked = true
                "C" -> holder.radioOptionC.isChecked = true
                "D" -> holder.radioOptionD.isChecked = true
            }
        }

        // Set listener for option selection
        holder.radioGroupOptions.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioOptionA -> selectedAnswers[question.id] = "A"
                R.id.radioOptionB -> selectedAnswers[question.id] = "B"
                R.id.radioOptionC -> selectedAnswers[question.id] = "C"
                R.id.radioOptionD -> selectedAnswers[question.id] = "D"
            }
        }
    }

    override fun getItemCount(): Int = questions.size

    /**
     * Get all selected answers as a Map<String, String> for API submission.
     * Format: {"questionId": "A", "questionId2": "B"}
     */
    fun getAnswers(): Map<String, String> {
        // Convert Map<Int, String> to Map<String, String>
        return selectedAnswers.mapKeys { it.key.toString() }
    }

    /**
     * Check if all questions have been answered.
     */
    fun areAllQuestionsAnswered(): Boolean {
        return selectedAnswers.size == questions.size
    }
}
