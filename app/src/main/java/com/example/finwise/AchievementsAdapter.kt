package com.example.finwise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.AchievementDef

/**
 * Adapter for displaying achievements in a RecyclerView.
 * Shows earned badges in full color, unearned badges grayed out with lock.
 */
class AchievementsAdapter(
    private val achievements: List<AchievementDef>,
    private val earnedKeys: List<String>
) : RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder>() {

    class AchievementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivBadgeIcon: ImageView = view.findViewById(R.id.ivBadgeIcon)
        val ivLock: ImageView = view.findViewById(R.id.ivLock)
        val tvBadgeName: TextView = view.findViewById(R.id.tvBadgeName)
        val tvBadgeDescription: TextView = view.findViewById(R.id.tvBadgeDescription)
        val tvBadgeXp: TextView = view.findViewById(R.id.tvBadgeXp)
        val ivEarned: ImageView = view.findViewById(R.id.ivEarned)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = achievements[position]
        val isEarned = earnedKeys.contains(achievement.key)

        holder.tvBadgeName.text = achievement.name
        holder.tvBadgeDescription.text = achievement.description
        holder.tvBadgeXp.text = "+${achievement.xp_reward} XP"

        if (isEarned) {
            // Earned - show checkmark, full colors
            holder.ivEarned.visibility = View.VISIBLE
            holder.ivLock.visibility = View.GONE
            holder.ivBadgeIcon.alpha = 1.0f
            holder.tvBadgeName.alpha = 1.0f
            holder.tvBadgeDescription.alpha = 1.0f
        } else {
            // Unearned - show lock, grayed out
            holder.ivEarned.visibility = View.GONE
            holder.ivLock.visibility = View.VISIBLE
            holder.ivBadgeIcon.alpha = 0.4f
            holder.tvBadgeName.alpha = 0.6f
            holder.tvBadgeDescription.alpha = 0.6f
        }

        // Set badge icon based on achievement key
        val iconRes = when (achievement.key) {
            "first_expense" -> R.drawable.ic_wallet
            "first_trade" -> R.drawable.ic_trending_up
            "week_streak" -> R.drawable.ic_star
            "month_streak" -> R.drawable.ic_badge
            "budget_master" -> R.drawable.ic_check_circle
            "diversifier" -> R.drawable.ic_trending_up
            else -> R.drawable.ic_badge
        }
        holder.ivBadgeIcon.setImageResource(iconRes)
    }

    override fun getItemCount(): Int = achievements.size
}
