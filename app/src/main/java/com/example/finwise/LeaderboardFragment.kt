package com.example.finwise

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.LeaderboardResponse
import com.example.finwise.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

/**
 * Fragment displaying the Hall of Fame leaderboard.
 * Shows Top 50 users and the current user's rank in a sticky footer.
 */
class LeaderboardFragment : Fragment() {

    private lateinit var progressLoading: ProgressBar
    private lateinit var rvLeaderboard: RecyclerView
    private lateinit var tvUserRank: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserXp: TextView
    private lateinit var ivUserProfile: ImageView

    private var userEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get user email
        val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)

        // Initialize views
        progressLoading = view.findViewById(R.id.progressLoading)
        rvLeaderboard = view.findViewById(R.id.rvLeaderboard)
        tvUserRank = view.findViewById(R.id.tvUserRank)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserXp = view.findViewById(R.id.tvUserXp)
        ivUserProfile = view.findViewById(R.id.ivUserProfile)

        // Set up RecyclerView
        rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())

        // Back button
        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Load leaderboard data
        userEmail?.let { email ->
            loadLeaderboard(email)
        } ?: run {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadLeaderboard(email: String) {
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getLeaderboard(email)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        updateUI(response)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        showLoading(false)
                        Toast.makeText(
                            requireContext(),
                            "Failed to load leaderboard: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun updateUI(response: LeaderboardResponse) {
        showLoading(false)

        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))

        // Update RecyclerView with top users
        rvLeaderboard.adapter = LeaderboardAdapter(response.top_users)

        // Update sticky footer with current user's rank
        tvUserRank.text = "#${response.user_rank}"
        tvUserName.text = response.user_display_name
        tvUserXp.text = "${formatter.format(response.user_xp)} XP"

        // Load user's profile picture
        if (!response.user_profile_picture.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(response.user_profile_picture, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ivUserProfile.setImageBitmap(bitmap)
                ivUserProfile.setPadding(0, 0, 0, 0)
            } catch (e: Exception) {
                // Keep default icon on error
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        progressLoading.visibility = if (loading) View.VISIBLE else View.GONE
        rvLeaderboard.visibility = if (loading) View.GONE else View.VISIBLE
    }
}
