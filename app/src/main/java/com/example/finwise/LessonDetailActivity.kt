package com.example.finwise

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.finwise.api.LessonCompleteRequest
import com.example.finwise.api.RetrofitClient
import com.example.finwise.databinding.ActivityLessonDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LessonDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLessonDetailBinding
    private var player: ExoPlayer? = null
    
    private var lessonId: Int = 0
    private var lessonTitle: String = ""
    private var lessonDescription: String = ""
    private var videoUrl: String = ""
    
    // Save playback position for orientation changes
    private var playbackPosition: Long = 0L
    private var playWhenReady: Boolean = true
    
    // Fullscreen state
    private var isFullscreen: Boolean = false
    
    // Gesture detector for double-tap skip
    private lateinit var gestureDetector: GestureDetectorCompat

    companion object {
        private const val TAG = "LessonDetailActivity"
        private const val KEY_LEARNINGS_SEPARATOR = "KEY LEARNINGS:"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLessonDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from intent
        lessonId = intent.getIntExtra("LESSON_ID", 0)
        lessonTitle = intent.getStringExtra("LESSON_TITLE") ?: ""
        lessonDescription = intent.getStringExtra("LESSON_SUBTITLE") ?: ""
        videoUrl = intent.getStringExtra("LESSON_VIDEO_URL") ?: ""

        displayLessonInfo()
        setupClickListeners()
        setupFullscreenButton()
        setupDoubleTapGesture()
    }

    private fun displayLessonInfo() {
        binding.tvLessonTitle.text = lessonTitle
        
        // Split description if it contains KEY LEARNINGS:
        if (lessonDescription.contains(KEY_LEARNINGS_SEPARATOR, ignoreCase = true)) {
            val parts = lessonDescription.split(KEY_LEARNINGS_SEPARATOR, ignoreCase = true, limit = 2)
            binding.tvLessonDescription.text = parts[0].trim()
            binding.tvKeyLearnings.text = if (parts.size > 1) parts[1].trim() else ""
        } else {
            // No KEY LEARNINGS section - use full description
            binding.tvLessonDescription.text = lessonDescription
            // Provide default key learnings
            binding.tvKeyLearnings.text = "• Complete this lesson to unlock key insights\n• Take the quiz to test your knowledge"
        }
    }

    private fun setupClickListeners() {
        binding.btnTakeQuiz.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java).apply {
                putExtra("LESSON_ID", lessonId)
                putExtra("LESSON_TITLE", lessonTitle)
            }
            startActivity(intent)
        }
    }

    private fun initializePlayer() {
        if (videoUrl.isEmpty()) {
            Log.e(TAG, "Video URL is empty!")
            Toast.makeText(this, "Video URL not available", Toast.LENGTH_SHORT).show()
            return
        }

        // Create ExoPlayer instance with 10-second seek increments
        player = ExoPlayer.Builder(this)
            .setSeekBackIncrementMs(10000) // 10 seconds backward
            .setSeekForwardIncrementMs(10000) // 10 seconds forward
            .build().also { exoPlayer ->
            // Bind player to the view
            binding.playerView.player = exoPlayer

            // Create media item from Firebase URL
            val mediaItem = MediaItem.fromUri(videoUrl)
            
            // Set media item and prepare
            exoPlayer.setMediaItem(mediaItem)
            
            // Restore playback state
            exoPlayer.seekTo(playbackPosition)
            exoPlayer.playWhenReady = playWhenReady
            
            // Add listener for errors
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Player error: ${error.message}")
                    Toast.makeText(
                        this@LessonDetailActivity,
                        "Error playing video: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> Log.d(TAG, "Buffering...")
                        Player.STATE_READY -> Log.d(TAG, "Ready to play")
                        Player.STATE_ENDED -> {
                            Log.d(TAG, "Playback ended — awarding XP")
                            awardXpForLesson()
                        }
                        Player.STATE_IDLE -> Log.d(TAG, "Player idle")
                    }
                }
            })
            
            // Prepare the player
            exoPlayer.prepare()
        }
        
        Log.d(TAG, "Player initialized with URL: $videoUrl")
    }

    /**
     * Called when video finishes playing.
     * Awards 100 XP by calling POST /api/learn/complete (backend prevents duplicates).
     */
    private fun awardXpForLesson() {
        val sharedPref = getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        val email = sharedPref.getString("LOGGED_IN_EMAIL", null) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.instance.completeLesson(
                    LessonCompleteRequest(email = email, video_id = lessonId)
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@LessonDetailActivity,
                        "🎉 +100 XP Earned!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to award XP: ${e.message}")
            }
        }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            // Save playback state before releasing
            playbackPosition = exoPlayer.currentPosition
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.release()
        }
        player = null
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        // Re-initialize if player was released
        if (player == null) {
            initializePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
    
    private fun setupFullscreenButton() {
        // Find fullscreen button in custom controller
        binding.playerView.findViewById<ImageButton>(R.id.exo_fullscreen)?.setOnClickListener {
            toggleFullscreen()
        }
        
        // Configure 10-second skip intervals
        binding.playerView.setControllerShowTimeoutMs(3000)
        binding.playerView.controllerHideOnTouch = true
    }
    
    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        
        if (isFullscreen) {
            enterFullscreen()
        } else {
            exitFullscreen()
        }
    }
    
    private fun enterFullscreen() {
        // Hide system UI
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        // Set landscape orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        // Hide action bar
        supportActionBar?.hide()
        
        // Update fullscreen button icon
        binding.playerView.findViewById<ImageButton>(R.id.exo_fullscreen)?.setImageResource(
            android.R.drawable.ic_menu_revert
        )
    }
    
    private fun exitFullscreen() {
        // Show system UI
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, binding.root).show(WindowInsetsCompat.Type.systemBars())
        
        // Set portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        // Show action bar
        supportActionBar?.show()
        
        // Update fullscreen button icon
        binding.playerView.findViewById<ImageButton>(R.id.exo_fullscreen)?.setImageResource(
            android.R.drawable.ic_menu_crop
        )
    }
    
    private fun setupDoubleTapGesture() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val screenWidth = binding.playerView.width
                val tapX = e.x
                
                player?.let { exoPlayer ->
                    // Don't seek if player is not ready
                    if (exoPlayer.playbackState == Player.STATE_IDLE || 
                        exoPlayer.playbackState == Player.STATE_ENDED) {
                        Log.d(TAG, "Player not ready for seeking")
                        return true
                    }
                    
                    val currentPosition = exoPlayer.currentPosition
                    val duration = exoPlayer.duration
                    
                    Log.d(TAG, "Double-tap detected at X: $tapX, screenWidth: $screenWidth")
                    Log.d(TAG, "Current position: $currentPosition ms, Duration: $duration ms, State: ${exoPlayer.playbackState}")
                    
                    when {
                        // Double-tap on left side - rewind 10 seconds
                        tapX < screenWidth / 2 -> {
                            val newPosition = maxOf(0L, currentPosition - 10000)
                            Log.d(TAG, "Seeking back from $currentPosition to $newPosition")
                            // Use seekTo with current media item to avoid restart
                            exoPlayer.seekTo(exoPlayer.currentMediaItemIndex, newPosition)
                            Toast.makeText(this@LessonDetailActivity, "⏪ -10s", Toast.LENGTH_SHORT).show()
                        }
                        // Double-tap on right side - forward 10 seconds
                        else -> {
                            val newPosition = currentPosition + 10000
                            Log.d(TAG, "Seeking forward from $currentPosition to $newPosition")
                            // Use seekTo with current media item to avoid restart
                            exoPlayer.seekTo(exoPlayer.currentMediaItemIndex, newPosition)
                            Toast.makeText(this@LessonDetailActivity, "⏩ +10s", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                
                return true
            }
            
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // Toggle controls visibility on single tap
                if (binding.playerView.isControllerFullyVisible) {
                    binding.playerView.hideController()
                } else {
                    binding.playerView.showController()
                }
                return true
            }
        })
        
        // Intercept touch events for double-tap detection only
        binding.playerView.setOnTouchListener { v, event ->
            // Check if it's a double-tap or single tap
            if (gestureDetector.onTouchEvent(event)) {
                // Gesture detected and handled
                true
            } else {
                // Let PlayerView handle other gestures
                v.performClick()
                false
            }
        }
    }
}
