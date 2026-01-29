package com.example.finwise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.SavingsGoalResponse
import com.google.android.material.button.MaterialButton

class SavingsGoalAdapter(
    private var goals: List<SavingsGoalResponse>,
    private val onDepositClick: (SavingsGoalResponse) -> Unit
) : RecyclerView.Adapter<SavingsGoalAdapter.GoalViewHolder>() {

    class GoalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivGoalIcon: ImageView = view.findViewById(R.id.ivGoalIcon)
        val tvGoalTitle: TextView = view.findViewById(R.id.tvGoalTitle)
        val tvDeadline: TextView = view.findViewById(R.id.tvDeadline)
        val progressGoal: ProgressBar = view.findViewById(R.id.progressGoal)
        val tvCurrentAmount: TextView = view.findViewById(R.id.tvCurrentAmount)
        val tvTargetAmount: TextView = view.findViewById(R.id.tvTargetAmount)
        val tvProgressPercent: TextView = view.findViewById(R.id.tvProgressPercent)
        val btnDeposit: MaterialButton = view.findViewById(R.id.btnDeposit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_savings_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]

        holder.tvGoalTitle.text = goal.title
        holder.tvCurrentAmount.text = "₹${String.format("%,.0f", goal.current_amount)}"
        holder.tvTargetAmount.text = "/ ₹${String.format("%,.0f", goal.target_amount)}"
        holder.tvProgressPercent.text = "(${goal.progress_percent}%)"
        holder.progressGoal.progress = goal.progress_percent

        // Show deadline if exists
        if (goal.deadline != null) {
            holder.tvDeadline.text = "Target: ${goal.deadline}"
            holder.tvDeadline.visibility = View.VISIBLE
        } else {
            holder.tvDeadline.visibility = View.GONE
        }

        // Change button text if goal is reached
        if (goal.progress_percent >= 100) {
            holder.btnDeposit.text = "Goal Reached! 🎉"
            holder.btnDeposit.isEnabled = false
        } else {
            holder.btnDeposit.text = "Deposit Funds"
            holder.btnDeposit.isEnabled = true
        }

        holder.btnDeposit.setOnClickListener {
            onDepositClick(goal)
        }
    }

    override fun getItemCount() = goals.size

    fun updateGoals(newGoals: List<SavingsGoalResponse>) {
        goals = newGoals
        notifyDataSetChanged()
    }
}
