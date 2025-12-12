package com.example.finwise

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.finwise.api.ProfileUpdate
import com.example.finwise.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnSave: TextView
    private lateinit var profileAvatarContainer: FrameLayout
    private lateinit var ivProfileAvatar: ImageView
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText

    private var userEmail: String? = null
    private var currentProfilePicture: String? = null
    private var newProfilePicture: String? = null

    // Image picker launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedImage(it) }
    }

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(this, "Permission required to select images", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Get user email from shared preferences
        val sharedPref = getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)

        // Initialize views
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSave)
        profileAvatarContainer = findViewById(R.id.profileAvatarContainer)
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)

        // Set email (read-only)
        etEmail.setText(userEmail ?: "")

        // Load current user data
        userEmail?.let { loadUserData(it) }

        // Setup click listeners
        setupClickListeners()
    }

    private fun loadUserData(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userProfile = RetrofitClient.instance.getUserDetails(email)
                withContext(Dispatchers.Main) {
                    etName.setText(userProfile.name)
                    currentProfilePicture = userProfile.profile_picture
                    loadProfilePicture(userProfile.profile_picture)
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
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
                // Keep default icon
            }
        }
    }

    private fun setupClickListeners() {
        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Save button
        btnSave.setOnClickListener {
            saveProfile()
        }

        // Profile avatar - open image picker
        profileAvatarContainer.setOnClickListener {
            checkPermissionAndPickImage()
        }
    }

    private fun saveProfile() {
        val newName = etName.text.toString().trim()
        
        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        userEmail?.let { email ->
            // Show loading state
            btnSave.isEnabled = false
            btnSave.text = "Saving..."

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = ProfileUpdate(
                        email = email,
                        name = newName,
                        profile_picture = newProfilePicture
                    )
                    RetrofitClient.instance.updateProfile(request)
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditProfileActivity, "Profile updated!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditProfileActivity, "Failed to update profile", Toast.LENGTH_SHORT).show()
                        btnSave.isEnabled = true
                        btnSave.text = "Save"
                    }
                }
            }
        }
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openImagePicker() {
        pickImageLauncher.launch("image/*")
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            // Resize the image
            val resizedBitmap = resizeBitmap(originalBitmap, 300)
            
            // Display the image
            ivProfileAvatar.setImageBitmap(resizedBitmap)
            ivProfileAvatar.setPadding(0, 0, 0, 0)
            ivProfileAvatar.imageTintList = null
            
            // Convert to Base64 and store for saving
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val imageBytes = outputStream.toByteArray()
            newProfilePicture = Base64.encodeToString(imageBytes, Base64.DEFAULT)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
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
}
