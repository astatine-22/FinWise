package com.example.finwise

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class LessonAdapter(
    private val lessons: List<Lesson>,
    private val onLessonClick: (Lesson) -> Unit
) : RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {

    class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardCourse)
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val tvCourseTitle: TextView = itemView.findViewById(R.id.tvCourseTitle)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvXpBadge: TextView = itemView.findViewById(R.id.tvXpBadge)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val btnStart: android.widget.Button = itemView.findViewById(R.id.btnStart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course_card, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessons[position]
        
        holder.tvCourseTitle.text = lesson.title
        holder.tvCategory.text = lesson.subtitle.uppercase()
        holder.tvXpBadge.text = "⭐ ${lesson.xp} XP"
        holder.tvDuration.text = "10 mins" // Placeholder/Default
        
        // Extract YouTube ID and load Thumbnail
        val videoId = getVideoIdFromUrl(lesson.videoUrl)
        if (videoId.isNotEmpty()) {
            val thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(thumbnailUrl)
                .placeholder(android.R.drawable.ic_menu_gallery) // Basic placeholder
                .error(android.R.drawable.ic_dialog_alert)
                .into(holder.ivThumbnail)
        }
        
        val onClick = View.OnClickListener {
            onLessonClick(lesson)
        }

        holder.cardView.setOnClickListener(onClick)
        holder.btnStart.setOnClickListener(onClick)
    }

    override fun getItemCount(): Int = lessons.size

    private fun getVideoIdFromUrl(url: String): String {
        return try {
            if (url.contains("/embed/")) {
                url.substringAfter("/embed/").substringBefore("?")
            } else if (url.contains("v=")) {
                url.substringAfter("v=").substringBefore("&")
            } else {
                url.substringAfterLast("/")
            }
        } catch (e: Exception) {
            ""
        }
    }
}
