package com.example.finwise

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.LearnVideoResponse
import com.example.finwise.api.RetrofitClient
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LearnFragment : Fragment() {

    private lateinit var rvVideos: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var adapter: LearnVideosAdapter

    private var allVideos: List<LearnVideoResponse> = emptyList()
    private var selectedCategory: String? = null

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
        setupChipGroup()
        loadVideos()
    }

    private fun initializeViews(view: View) {
        rvVideos = view.findViewById(R.id.rvVideos)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories)
    }

    private fun setupRecyclerView() {
        adapter = LearnVideosAdapter(emptyList()) { video ->
            openYouTubeVideo(video.youtube_video_id)
        }
        rvVideos.layoutManager = LinearLayoutManager(context)
        rvVideos.adapter = adapter
    }

    private fun setupChipGroup() {
        chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                // "All" is unselected, show all videos
                selectedCategory = null
                filterVideos()
            } else {
                val chipId = checkedIds[0]
                val chip = chipGroupCategories.findViewById<Chip>(chipId)
                val category = chip?.text?.toString()
                
                if (category == "All") {
                    selectedCategory = null
                } else {
                    selectedCategory = category
                }
                filterVideos()
            }
        }
    }

    private fun loadVideos() {
        progressBar.visibility = View.VISIBLE
        rvVideos.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val videos = RetrofitClient.instance.getLearnVideos()
                val categories = videos.map { it.category }.distinct()

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    allVideos = videos
                    progressBar.visibility = View.GONE

                    // Add category chips dynamically
                    addCategoryChips(categories)

                    if (videos.isEmpty()) {
                        emptyStateLayout.visibility = View.VISIBLE
                        rvVideos.visibility = View.GONE
                    } else {
                        emptyStateLayout.visibility = View.GONE
                        rvVideos.visibility = View.VISIBLE
                        adapter.updateData(videos)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    progressBar.visibility = View.GONE
                    emptyStateLayout.visibility = View.VISIBLE
                    rvVideos.visibility = View.GONE
                    Toast.makeText(context, "Failed to load videos: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addCategoryChips(categories: List<String>) {
        // Remove all chips except "All"
        val chipAll = chipGroupCategories.findViewById<Chip>(R.id.chipAll)
        chipGroupCategories.removeAllViews()
        chipGroupCategories.addView(chipAll)

        // Add category chips
        categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category
                isCheckable = true
                isCheckedIconVisible = false
                setChipBackgroundColorResource(R.color.chip_background_selector)
            }
            chipGroupCategories.addView(chip)
        }
    }

    private fun filterVideos() {
        val filteredVideos = if (selectedCategory == null) {
            allVideos
        } else {
            allVideos.filter { it.category == selectedCategory }
        }

        if (filteredVideos.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            rvVideos.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            rvVideos.visibility = View.VISIBLE
        }
        
        adapter.updateData(filteredVideos)
    }

    private fun openYouTubeVideo(videoId: String) {
        // Try to open in YouTube app first
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))

        try {
            startActivity(appIntent)
        } catch (e: Exception) {
            // YouTube app not installed, open in browser
            startActivity(webIntent)
        }
    }
}
