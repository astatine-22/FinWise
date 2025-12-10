package com.example.finwise

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.ExpenseResponse
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ExpensesAdapter(
    private var expenses: List<ExpenseResponse>,
    private val onItemClick: (ExpenseResponse) -> Unit
) : RecyclerView.Adapter<ExpensesAdapter.ExpenseViewHolder>() {

    // ViewHolder class holding references to the UI views for each item
    class ExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view.findViewById(R.id.cardExpense)
        val iconContainer: View = view.findViewById(R.id.iconContainer)
        val tvCategoryIcon: TextView = view.findViewById(R.id.tvCategoryIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvExpenseTitle)
        val tvDate: TextView = view.findViewById(R.id.tvExpenseDate)
        val tvAmount: TextView = view.findViewById(R.id.tvExpenseAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        // Inflate the layout for this fragment
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]

        // Set Title
        holder.tvTitle.text = expense.title

        // Set Amount - Format to currency and add negative sign
        val rupeeFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
        val formattedAmount = rupeeFormat.format(expense.amount)
        holder.tvAmount.text = "-$formattedAmount"

        // IMPORTANT: Force dark color to match design, overriding any default red
        holder.tvAmount.setTextColor(Color.parseColor("#333333"))


        // Format the date string (e.g., "2023-10-27T10:00:00Z" -> "Oct 27")
        holder.tvDate.text = formatDate(expense.date)

        // Set pastel category icon and color background
        val (icon, colorHex) = getCategoryDetails(expense.category)
        holder.tvCategoryIcon.text = icon
        holder.iconContainer.background.setTint(Color.parseColor(colorHex))

        // Set click listener for the entire card
        holder.cardView.setOnClickListener {
            onItemClick(expense)
        }
    }

    override fun getItemCount() = expenses.size

    // Helper function to update the data list and refresh the view
    fun updateData(newExpenses: List<ExpenseResponse>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }

    // Helper function to format the raw date string from the API
    private fun formatDate(dateString: String): String {
        try {
            // Input format matching the typical API output (ISO 8601)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateString)

            // Desired output format (e.g., "Oct 27")
            val outputFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            return outputFormat.format(date!!)
        } catch (e: Exception) {
            // Fallback if parsing fails, try a simpler format or return original
            try {
                val simplerInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                simplerInput.timeZone = TimeZone.getTimeZone("UTC")
                val date = simplerInput.parse(dateString)
                val outputFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                return outputFormat.format(date!!)
            } catch(e2: Exception) {
                return dateString.substringBefore("T") // Return just YYYY-MM-DD if all else fails
            }
        }
    }

    // Helper function to get icon and pastel color for a category
    private fun getCategoryDetails(category: String): Pair<String, String> {
        return when (category.lowercase()) {
            "food", "food & drink" -> Pair("🍔", "#FFB74D") // Pastel Orange
            "transport" -> Pair("🚌", "#4FC3F7") // Pastel Light Blue
            "shopping" -> Pair("🛍️", "#CE93D8") // Pastel Purple
            "entertainment" -> Pair("🎬", "#80CBC4") // Pastel Teal
            "health" -> Pair("🏥", "#F48FB1") // Pastel Pink
            "utilities" -> Pair("💡", "#E57373") // Pastel Red
            "housing" -> Pair("🏠", "#A1887F") // Pastel Brown
            else -> Pair("💰", "#FFF176") // Pastel Yellow for others
        }
    }
}