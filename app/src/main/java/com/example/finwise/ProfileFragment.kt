package com.example.finwise

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.finwise.api.ProfilePictureUpdate
import com.example.finwise.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnMore: ImageView
    private lateinit var profileAvatarContainer: FrameLayout
    private lateinit var ivProfileAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserHandle: TextView
    private lateinit var tvTotalXp: TextView
    private lateinit var tvBadges: TextView
    private lateinit var tvModules: TextView
    private lateinit var btnEditProfile: LinearLayout
    private lateinit var btnSettings: LinearLayout
    private lateinit var btnLogout: TextView

    private var userEmail: String? = null

    // Image picker launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            handleSelectedImage(it)
        }
    }

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(requireContext(), "Permission required to select images", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get user email from shared preferences
        val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)

        // Initialize views
        btnBack = view.findViewById(R.id.btnBack)
        btnMore = view.findViewById(R.id.btnMore)
        profileAvatarContainer = view.findViewById(R.id.profileAvatarContainer)
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserHandle = view.findViewById(R.id.tvUserHandle)
        tvTotalXp = view.findViewById(R.id.tvTotalXp)
        tvBadges = view.findViewById(R.id.tvBadges)
        tvModules = view.findViewById(R.id.tvModules)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnSettings = view.findViewById(R.id.btnSettings)
        btnLogout = view.findViewById(R.id.btnLogout)

        // Set user handle from email
        userEmail?.let {
            val handle = "@" + it.substringBefore("@")
            tvUserHandle.text = handle
        }

        // Fetch and display user data (including profile picture from backend)
        userEmail?.let { fetchUserData(it) }

        // Setup click listeners
        setupClickListeners()
    }

    private fun loadProfilePicture(base64Image: String?) {
        if (base64Image != null && base64Image.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ivProfileAvatar.setImageBitmap(bitmap)
                ivProfileAvatar.setPadding(0, 0, 0, 0)
                ivProfileAvatar.imageTintList = null
            } catch (e: Exception) {
                // If decoding fails, keep the default icon
            }
        }
    }

    private fun uploadProfilePicture(base64Image: String) {
        userEmail?.let { email ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = ProfilePictureUpdate(email = email, profile_picture = base64Image)
                    RetrofitClient.instance.updateProfilePicture(request)
                    
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Profile picture saved!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            // Save locally as fallback
                            val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
                            sharedPref.edit().putString("PROFILE_PICTURE", base64Image).apply()
                            Toast.makeText(requireContext(), "Saved locally (offline mode)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            // Resize the image to reduce storage size (max 300x300)
            val resizedBitmap = resizeBitmap(originalBitmap, 300)
            
            // Display the image
            ivProfileAvatar.setImageBitmap(resizedBitmap)
            ivProfileAvatar.setPadding(0, 0, 0, 0)
            ivProfileAvatar.imageTintList = null
            
            // Convert to Base64
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val imageBytes = outputStream.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)
            
            // Upload to backend
            uploadProfilePicture(base64Image)
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resizeBitmap(source: Bitmap, maxSize: Int): Bitmap {
        val width = source.width
        val height = source.height
        
        val ratio = width.toFloat() / height.toFloat()
        
        val newWidth: Int
        val newHeight: Int
        
        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }
        
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(requireContext(), "Permission needed to select profile picture", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(permission)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openImagePicker() {
        pickImageLauncher.launch("image/*")
    }

    private fun fetchUserData(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userProfile = RetrofitClient.instance.getUserDetails(email)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        tvUserName.text = userProfile.name
                        tvTotalXp.text = userProfile.xp.toString()
                        tvBadges.text = "5"
                        tvModules.text = "3"
                        
                        // Load profile picture from backend
                        loadProfilePicture(userProfile.profile_picture)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        tvUserName.text = "User"
                        tvTotalXp.text = "0"
                        tvBadges.text = "0"
                        tvModules.text = "0"
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        // Back button - finish the activity
        btnBack.setOnClickListener {
            requireActivity().finish()
        }

        // More options button
        btnMore.setOnClickListener {
            Toast.makeText(requireContext(), "More options coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Profile Avatar - Open image picker
        profileAvatarContainer.setOnClickListener {
            checkPermissionAndPickImage()
        }

        // Edit Profile - Navigate to EditProfileActivity
        btnEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Settings
        btnSettings.setOnClickListener {
            Toast.makeText(requireContext(), "Settings coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Logout
        btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        // Clear shared preferences
        val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        // Navigate to Login screen and clear the back stack
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        // Finish current activity
        requireActivity().finish()
    }
}
