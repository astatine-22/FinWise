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
        val cardView: MaterialCardView = itemView as MaterialCardView
        val ivLessonIcon: ImageView = itemView.findViewById(R.id.ivLessonIcon)
        val tvLessonTitle: TextView = itemView.findViewById(R.id.tvLessonTitle)
        val tvLessonSubtitle: TextView = itemView.findViewById(R.id.tvLessonSubtitle)
        val tvLessonXP: TextView = itemView.findViewById(R.id.tvLessonXP)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessons[position]
        
        holder.tvLessonTitle.text = lesson.title
        holder.tvLessonSubtitle.text = lesson.subtitle
        holder.tvLessonXP.text = "+${lesson.xp} XP"
        
        holder.cardView.setOnClickListener {
            onLessonClick(lesson)
        }
    }

    override fun getItemCount(): Int = lessons.size
}
