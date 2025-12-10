package com.example.finwise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.LearnVideoResponse
import com.google.android.material.card.MaterialCardView

class LearnVideosAdapter(
    private var videos: List<LearnVideoResponse>,
    private val onVideoClick: (LearnVideoResponse) -> Unit
) : RecyclerView.Adapter<LearnVideosAdapter.VideoViewHolder>() {

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView as MaterialCardView
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvFeatured: TextView = itemView.findViewById(R.id.tvFeatured)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_learn_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]

        // Set video info
        holder.tvTitle.text = video.title
        holder.tvDescription.text = video.description ?: "Learn more about ${video.category}"
        holder.tvCategory.text = video.category

        // Format and display duration
        video.duration_minutes?.let { minutes ->
            holder.tvDuration.text = "$minutes min"
            holder.tvDuration.visibility = View.VISIBLE
        } ?: run {
            holder.tvDuration.visibility = View.GONE
        }

        // Show/hide featured badge
        holder.tvFeatured.visibility = if (video.is_featured) View.VISIBLE else View.GONE

        // Load thumbnail using Glide or Picasso if available
        // For now, we'll try to load using a simple approach
        video.thumbnail_url?.let { url ->
            try {
                // Using a simple image loading approach with Coil or just set placeholder
                // In production, use Glide or Coil
                loadThumbnail(holder.ivThumbnail, url)
            } catch (e: Exception) {
                holder.ivThumbnail.setBackgroundColor(0xFFE0E0E0.toInt())
            }
        } ?: run {
            // Set a colored placeholder based on category
            val colorRes = getCategoryColor(video.category)
            holder.ivThumbnail.setBackgroundColor(colorRes)
        }

        // Set category chip color
        setCategoryChipColor(holder.tvCategory, video.category)

        // Click listener
        holder.cardView.setOnClickListener {
            onVideoClick(video)
        }
    }

    override fun getItemCount(): Int = videos.size

    fun updateData(newVideos: List<LearnVideoResponse>) {
        videos = newVideos
        notifyDataSetChanged()
    }

    private fun loadThumbnail(imageView: ImageView, url: String) {
        // Simple thumbnail loading using a background thread
        // In production, use Glide: Glide.with(imageView.context).load(url).into(imageView)
        Thread {
            try {
                val inputStream = java.net.URL(url).openStream()
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                imageView.post {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                imageView.post {
                    imageView.setBackgroundColor(0xFFE0E0E0.toInt())
                }
            }
        }.start()
    }

    private fun getCategoryColor(category: String): Int {
        return when (category.lowercase()) {
            "investing basics" -> 0xFF4CAF50.toInt() // Green
            "mutual funds" -> 0xFF2196F3.toInt() // Blue
            "crypto" -> 0xFFFF9800.toInt() // Orange
            "budgeting" -> 0xFF9C27B0.toInt() // Purple
            "tax planning" -> 0xFF795548.toInt() // Brown
            else -> 0xFF607D8B.toInt() // Blue Grey
        }
    }

    private fun setCategoryChipColor(chipView: TextView, category: String) {
        val (bgColor, textColor) = when (category.lowercase()) {
            "investing basics" -> Pair(0xFFE8F5E9.toInt(), 0xFF4CAF50.toInt())
            "mutual funds" -> Pair(0xFFE3F2FD.toInt(), 0xFF2196F3.toInt())
            "crypto" -> Pair(0xFFFFF3E0.toInt(), 0xFFFF9800.toInt())
            "budgeting" -> Pair(0xFFF3E5F5.toInt(), 0xFF9C27B0.toInt())
            "tax planning" -> Pair(0xFFEFEBE9.toInt(), 0xFF795548.toInt())
            else -> Pair(0xFFECEFF1.toInt(), 0xFF607D8B.toInt())
        }
        
        chipView.setBackgroundColor(bgColor)
        chipView.setTextColor(textColor)
    }
}
