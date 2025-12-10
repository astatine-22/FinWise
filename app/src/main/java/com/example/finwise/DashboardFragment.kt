package com.example.finwise

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.finwise.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardFragment : Fragment() {

    private lateinit var tvXp: TextView
    private var userEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Get User Email from shared preferences
        val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)

        // 2. Initialize Views that actually exist in your current XML
        tvXp = view.findViewById(R.id.tvXp)
        val cardBudget = view.findViewById<CardView>(R.id.cardBudget)

        // 3. Load Data (Only XP for now, as other views are missing)
        userEmail?.let {
            fetchUserData(it)
        }

        // 4. Navigation to Budget tab
        cardBudget.setOnClickListener {
            // Find the ViewPager in the parent Activity and move to index 1 (Budget)
            val viewPager = requireActivity().findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
            viewPager.currentItem = 1
        }
    }

    private fun fetchUserData(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // We use the same getUserDetails endpoint to get XP
                val userProfile = RetrofitClient.instance.getUserDetails(email)
                withContext(Dispatchers.Main) {
                    if(isAdded) {
                        tvXp.text = userProfile.xp.toString()
                        // Note: Your current XML doesn't have a place for the Welcome message,
                        // so we can't set userProfile.name anywhere yet.
                    }
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}