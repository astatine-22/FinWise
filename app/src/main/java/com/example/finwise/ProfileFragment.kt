package com.example.finwise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.finwise.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileHandle: TextView
    private lateinit var btnLogout: View
    private lateinit var btnBack: ImageView

    // New Menu Options
    private lateinit var btnEditProfile: View
    private lateinit var btnSettings: View
    private lateinit var btnHelp: View
    private lateinit var btnPrivacy: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Views
        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvProfileHandle = view.findViewById(R.id.tvProfileHandle)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnBack = view.findViewById(R.id.btnBack)

        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnSettings = view.findViewById(R.id.btnSettings)
        btnHelp = view.findViewById(R.id.btnHelp)
        btnPrivacy = view.findViewById(R.id.btnPrivacy)

        // 1. Load User Data
        loadUserData()

        // 2. Set up Click Listeners
        setupClickListeners()
    }

    private fun loadUserData() {
        val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        val savedEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)

        savedEmail?.let { email ->
            // Create a handle from the email (e.g., jordan@mail.com -> @jordan)
            val handle = "@" + email.substringBefore("@")
            tvProfileHandle.text = handle

            // Fetch full name from backend
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userProfile = RetrofitClient.instance.getUserDetails(email)
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            tvProfileName.text = userProfile.name
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        if (isAdded) tvProfileName.text = "User"
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        // Back button (for now just shows a toast as it's a top-level fragment)
        btnBack.setOnClickListener {
            Toast.makeText(requireContext(), "Back clicked", Toast.LENGTH_SHORT).show()
        }

        // Menu options placeholders
        val menuListener = View.OnClickListener { view ->
            val action = when(view.id) {
                R.id.btnEditProfile -> "Edit Profile"
                R.id.btnSettings -> "Settings"
                R.id.btnHelp -> "Help & Support"
                R.id.btnPrivacy -> "Privacy Policy"
                else -> ""
            }
            Toast.makeText(requireContext(), "$action Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        btnEditProfile.setOnClickListener(menuListener)
        btnSettings.setOnClickListener(menuListener)
        btnHelp.setOnClickListener(menuListener)
        btnPrivacy.setOnClickListener(menuListener)

        // Logout Logic
        btnLogout.setOnClickListener {
            val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
            sharedPref.edit().clear().apply()

            val intent = Intent(requireActivity(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
    }
}