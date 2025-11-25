package com.example.finwise

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.ExpenseResponse
import java.text.NumberFormat
import java.util.Locale

class ExpenseAdapter(private var expenses: List<ExpenseResponse>) :
    RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    class ExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivExpenseIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvExpenseTitle)
        val tvDate: TextView = view.findViewById(R.id.tvExpenseDate)
        val tvAmount: TextView = view.findViewById(R.id.tvExpenseAmount)
        val iconBackground: View = view.findViewById(R.id.iconBackground)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]

        holder.tvTitle.text = expense.title
        holder.tvDate.text = expense.date

        // Format amount to Rupee currency
        val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        holder.tvAmount.text = "-${rupeeFormat.format(expense.amount)}"

        // Set icon and color based on category
        val (iconRes, colorHex) = when (expense.category) {
            "Food" -> Pair(R.drawable.ic_food, "#FFB74D") // Orange
            "Shopping" -> Pair(R.drawable.ic_shopping, "#CE93D8") // Purple
            "Transport" -> Pair(R.drawable.ic_transport, "#4FC3F7") // Light Blue
            "Entertainment" -> Pair(R.drawable.ic_entertainment, "#80CBC4") // Teal
            "Utilities" -> Pair(R.drawable.ic_utilities, "#E57373") // Red
            else -> Pair(R.drawable.ic_other, "#FFF176") // Yellow (Other)
        }

        holder.ivIcon.setImageResource(iconRes)
        holder.iconBackground.background.setTint(Color.parseColor(colorHex))
    }

    override fun getItemCount() = expenses.size

    // --- THIS IS THE FUNCTION THAT WAS MISSING ---
    // It updates the data list and refreshes the RecyclerView
    fun updateData(newExpenses: List<ExpenseResponse>) {
        this.expenses = newExpenses
        notifyDataSetChanged()
    }
}