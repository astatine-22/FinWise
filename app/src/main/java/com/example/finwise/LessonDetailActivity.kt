package com.example.finwise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.finwise.databinding.ActivityLessonDetailBinding

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

        // Create ExoPlayer instance
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
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
                        Player.STATE_ENDED -> Log.d(TAG, "Playback ended")
                        Player.STATE_IDLE -> Log.d(TAG, "Player idle")
                    }
                }
            })
            
            // Prepare the player
            exoPlayer.prepare()
        }
        
        Log.d(TAG, "Player initialized with URL: $videoUrl")
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
}
