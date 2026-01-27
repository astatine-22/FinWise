package com.example.finwise

import android.animation.ObjectAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class QuizBottomSheet : BottomSheetDialogFragment() {

    private lateinit var radioGroupOptions: RadioGroup
    private lateinit var rbOption1: RadioButton
    private lateinit var rbOption2: RadioButton
    private lateinit var rbOption3: RadioButton
    private lateinit var rbOption4: RadioButton
    private lateinit var btnSubmitAnswer: Button
    private lateinit var cvSuccessOverlay: CardView
    private lateinit var btnClaimReward: Button
    
    // Store the correct answer option ("A", "B", "C", or "D")
    private var correctAnswer: String = "A"

    // Interface to communicate with parent activity
    interface QuizCompletionListener {
        fun onQuizCompleted(xpEarned: Int)
    }

    private var listener: QuizCompletionListener? = null

    fun setQuizCompletionListener(listener: QuizCompletionListener) {
        this.listener = listener
    }
    
    // Set the correct answer (call this before showing the bottom sheet)
    fun setCorrectAnswer(answer: String) {
        this.correctAnswer = answer.uppercase()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_quiz_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        radioGroupOptions = view.findViewById(R.id.radioGroupOptions)
        rbOption1 = view.findViewById(R.id.rbOption1)
        rbOption2 = view.findViewById(R.id.rbOption2)
        rbOption3 = view.findViewById(R.id.rbOption3)
        rbOption4 = view.findViewById(R.id.rbOption4)
        btnSubmitAnswer = view.findViewById(R.id.btnSubmitAnswer)
        cvSuccessOverlay = view.findViewById(R.id.cvSuccessOverlay)
        btnClaimReward = view.findViewById(R.id.btnClaimReward)

        // Set up submit button click listener
        btnSubmitAnswer.setOnClickListener {
            handleSubmit()
        }

        // Set up claim reward button click listener
        btnClaimReward.setOnClickListener {
            listener?.onQuizCompleted(50)
            dismiss()
        }
    }

    private fun handleSubmit() {
        // Get the selected radio button ID
        val selectedId = radioGroupOptions.checkedRadioButtonId

        // Check if any option is selected
        if (selectedId == -1) {
            Toast.makeText(requireContext(), "Please select an answer!", Toast.LENGTH_SHORT).show()
            return
        }

        // Map the selected radio button to its corresponding option letter
        val selectedOption = when (selectedId) {
            R.id.rbOption1 -> "A"
            R.id.rbOption2 -> "B"
            R.id.rbOption3 -> "C"
            R.id.rbOption4 -> "D"

            else -> ""
        }

        // Check if the selected option matches the correct answer
        if (selectedOption == correctAnswer) {
            // Correct answer
            showSuccessOverlay()
            playSuccessSound()
        } else {
            // Wrong answer
            Toast.makeText(requireContext(), "Try again!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuccessOverlay() {
        cvSuccessOverlay.visibility = View.VISIBLE

        // Fade in animation
        cvSuccessOverlay.alpha = 0f
        ObjectAnimator.ofFloat(cvSuccessOverlay, "alpha", 0f, 1f).apply {
            duration = 500
            start()
        }

        // Scale animation for a pop-in effect
        cvSuccessOverlay.scaleX = 0.5f
        cvSuccessOverlay.scaleY = 0.5f
        ObjectAnimator.ofFloat(cvSuccessOverlay, "scaleX", 0.5f, 1f).apply {
            duration = 300
            start()
        }
        ObjectAnimator.ofFloat(cvSuccessOverlay, "scaleY", 0.5f, 1f).apply {
            duration = 300
            start()
        }
    }

    private fun playSuccessSound() {
        try {
            // Play a default notification sound
            val mediaPlayer = MediaPlayer.create(requireContext(), android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            mediaPlayer?.apply {
                setOnCompletionListener { mp ->
                    mp.release()
                }
                start()
            }
        } catch (e: Exception) {
            // Silently fail if sound cannot be played
            e.printStackTrace()
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
