package com.example.finwise

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.LeaderboardEntry
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter for displaying leaderboard entries in the Hall of Fame.
 * Top 3 ranks have special gold/silver/bronze styling.
 * Displays profile pictures if available.
 */
class LeaderboardAdapter(
    private val entries: List<LeaderboardEntry>
) : RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    class LeaderboardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val ivProfile: ImageView = view.findViewById(R.id.ivProfile)
        val tvDisplayName: TextView = view.findViewById(R.id.tvDisplayName)
        val tvXpLabel: TextView = view.findViewById(R.id.tvXpLabel)
        val tvXpValue: TextView = view.findViewById(R.id.tvXpValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard_user, parent, false)
        return LeaderboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val entry = entries[position]
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))

        holder.tvRank.text = entry.rank.toString()
        holder.tvDisplayName.text = entry.display_name
        holder.tvXpLabel.text = "${formatter.format(entry.xp)} XP"
        holder.tvXpValue.text = formatter.format(entry.xp)

        // Load profile picture if available
        if (!entry.profile_picture.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(entry.profile_picture, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.ivProfile.setImageBitmap(bitmap)
                holder.ivProfile.setPadding(0, 0, 0, 0)
                holder.ivProfile.imageTintList = null
            } catch (e: Exception) {
                // Use default icon if decoding fails
                holder.ivProfile.setImageResource(R.drawable.ic_person)
                holder.ivProfile.setPadding(8, 8, 8, 8)
            }
        } else {
            // Use default icon
            holder.ivProfile.setImageResource(R.drawable.ic_person)
            holder.ivProfile.setPadding(8, 8, 8, 8)
        }

        // Special colors for top 3 ranks
        when (entry.rank) {
            1 -> {
                // Gold for 1st place
                holder.tvRank.setBackgroundResource(R.drawable.bg_rank_gold)
                holder.tvDisplayName.setTextColor(Color.parseColor("#D4AF37"))
            }
            2 -> {
                // Silver for 2nd place
                holder.tvRank.setBackgroundResource(R.drawable.bg_rank_silver)
                holder.tvDisplayName.setTextColor(Color.parseColor("#C0C0C0"))
            }
            3 -> {
                // Bronze for 3rd place
                holder.tvRank.setBackgroundResource(R.drawable.bg_rank_bronze)
                holder.tvDisplayName.setTextColor(Color.parseColor("#CD7F32"))
            }
            else -> {
                // Default styling
                holder.tvRank.setBackgroundResource(R.drawable.bg_rank_badge)
                holder.tvDisplayName.setTextColor(Color.parseColor("#212121"))
            }
        }
    }

    override fun getItemCount(): Int = entries.size
}
