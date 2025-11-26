package com.example.finwise

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
// Note: Retrofit and Coroutines imports are removed as they aren't needed here anymore

class DashboardFragment : Fragment() {

    private lateinit var tvXp: TextView
    // tvWelcome is removed because it's not in this fragment's layout
    private var userEmail: String? = null

    // 1. Inflate the layout for this fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Tell the fragment which XML layout to use
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    // 2. Initialize views and start logic after view is created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get user email from the parent Activity intent
        userEmail = requireActivity().intent.getStringExtra("USER_EMAIL")

        // Initialize Views that ARE in this fragment layout
        tvXp = view.findViewById(R.id.tvXp)
        val cardBudget = view.findViewById<CardView>(R.id.cardBudget)

        // FOR NOW: We will just set a hardcoded XP value.
        // Tomorrow, when we do gamification, we will fetch real XP data here.
        tvXp.text = "150" // Placeholder value

        // Set click listener for the Budget card to open the Budget screen
        cardBudget.setOnClickListener {
            // Use "requireContext()" for intents within fragments
            val intent = Intent(requireContext(), BudgetActivity::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
        }
    }
}