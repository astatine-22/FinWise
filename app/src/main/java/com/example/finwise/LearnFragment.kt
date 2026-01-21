package com.example.finwise

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

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
        setupRecyclerView()
        loadDemoLessons()
    }

    private fun initializeViews(view: View) {
        rvLessons = view.findViewById(R.id.rvVideos)
    }

    private fun setupRecyclerView() {
        adapter = LessonAdapter(getDemoLessons()) { lesson ->
            openLessonDetail(lesson)
        }
        rvLessons.layoutManager = LinearLayoutManager(context)
        rvLessons.adapter = adapter
    }

    private fun loadDemoLessons() {
        // Data is already loaded from getDemoLessons()
        // No API calls needed for demo data
    }

    private fun getDemoLessons(): List<Lesson> {
        return listOf(
            Lesson(
                title = "Stock Market Basics",
                subtitle = "Investing 101",
                xp = "50",
                videoUrl = "https://www.youtube.com/embed/p7HKvqRI_Bo"
            ),
            Lesson(
                title = "Crypto for Beginners",
                subtitle = "Blockchain 101",
                xp = "100",
                videoUrl = "https://www.youtube.com/embed/Yb6825iv0Vk"
            ),
            Lesson(
                title = "How to Budget",
                subtitle = "Personal Finance",
                xp = "75",
                videoUrl = "https://www.youtube.com/embed/HQzoZfc3GwQ"
            ),
            Lesson(
                title = "Understanding Mutual Funds",
                subtitle = "Investment Strategies",
                xp = "60",
                videoUrl = "https://www.youtube.com/embed/dFj9UKsN5j4"
            ),
            Lesson(
                title = "Passive Income Strategies",
                subtitle = "Building Wealth",
                xp = "80",
                videoUrl = "https://www.youtube.com/embed/mQehUDW6jKA"
            )
        )
    }

    private fun openLessonDetail(lesson: Lesson) {
        val intent = Intent(requireContext(), LessonDetailActivity::class.java).apply {
            putExtra("VIDEO_URL", lesson.videoUrl)
            putExtra("TITLE", lesson.title)
            putExtra("SUBTITLE", lesson.subtitle)
        }
        startActivity(intent)
    }
}
