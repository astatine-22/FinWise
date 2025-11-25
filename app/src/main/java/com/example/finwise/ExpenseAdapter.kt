package com.example.finwise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.ExpenseItem  // <--- IMPORT THIS!

// NOTE: We deleted the "data class ExpenseItem" from here because
// we are using the one defined in ApiService.kt now.

class ExpenseAdapter(private val expenses: List<ExpenseItem>) :
    RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    class ExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTitle)
        val date: TextView = view.findViewById(R.id.tvDate)
        val amount: TextView = view.findViewById(R.id.tvAmount)
        val icon: ImageView = view.findViewById(R.id.imgCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val item = expenses[position]

        holder.title.text = item.title
        holder.date.text = item.date
        holder.amount.text = "- ₹${item.amount}"

        // Set Icon based on category
        when (item.category) {
            "Food" -> holder.icon.setImageResource(R.drawable.ic_food)
            "Shopping" -> holder.icon.setImageResource(R.drawable.ic_shopping)
            "Transport" -> holder.icon.setImageResource(R.drawable.ic_transport)
            else -> holder.icon.setImageResource(R.drawable.ic_entertainment)
        }
    }

    override fun getItemCount() = expenses.size
}