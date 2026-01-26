package com.example.finwise

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LearnFragment : Fragment() {

    private lateinit var rvLessons: RecyclerView
    private lateinit var adapter: LessonAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_learn, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupRecyclerView() // Set up with empty/demo data initially
        fetchVideos()
    }

    private fun initializeViews(view: View) {
        rvLessons = view.findViewById(R.id.rvVideos)
    }

    private fun setupRecyclerView() {
        rvLessons.layoutManager = LinearLayoutManager(context)
        // Initialize with empty list prevents errors before data loads
        adapter = LessonAdapter(emptyList()) { lesson ->
            openLessonDetail(lesson)
        }
        rvLessons.adapter = adapter
    }

    private fun fetchVideos() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch videos from API
                val videos = RetrofitClient.instance.getLearnVideos()
                
                // Map to Lesson objects
                val lessons = videos.map { video ->
                    Lesson(
                        id = video.id,
                        title = video.title,
                        subtitle = video.category,
                        xp = "50", // Placeholder XP or derived from backend if available
                        videoUrl = video.embed_url ?: "https://www.youtube.com/embed/${video.youtube_video_id}"
                    )
                }

                withContext(Dispatchers.Main) {
                    val sortedLessons = lessons.sortedBy { it.id } // Ensure order
                    adapter = LessonAdapter(sortedLessons) { lesson ->
                        openLessonDetail(lesson)
                    }
                    rvLessons.adapter = adapter
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to demo data on error (optional, or show toast)
                withContext(Dispatchers.Main) {
                     android.widget.Toast.makeText(context, "Failed to load videos", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openLessonDetail(lesson: Lesson) {
        val intent = Intent(requireContext(), LessonDetailActivity::class.java).apply {
            putExtra("VIDEO_URL", lesson.videoUrl)
            putExtra("VIDEO_ID", lesson.id) // Pass ID for Quiz
            putExtra("TITLE", lesson.title)
            putExtra("SUBTITLE", lesson.subtitle)
        }
        startActivity(intent)
    }
}
