package com.example.finwise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

// Helper data class for the breakdown items
data class BreakdownItem(
    val category: String,
    val amount: Float,
    val color: Int,
    val percentage: Int
)

class CategoryBreakdownAdapter(private val items: List<BreakdownItem>) :
    RecyclerView.Adapter<CategoryBreakdownAdapter.BreakdownViewHolder>() {

    class BreakdownViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewColorIndicator: View = view.findViewById(R.id.viewColorIndicator)
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvPercentage: TextView = view.findViewById(R.id.tvPercentage)
        val tvCategoryAmount: TextView = view.findViewById(R.id.tvCategoryAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreakdownViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_breakdown, parent, false)
        return BreakdownViewHolder(view)
    }

    override fun onBindViewHolder(holder: BreakdownViewHolder, position: Int) {
        val item = items[position]

        holder.tvCategoryName.text = item.category
        holder.tvPercentage.text = "${item.percentage}%"

        val rupeeFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
        holder.tvCategoryAmount.text = rupeeFormat.format(item.amount)

        // Set the color dot
        holder.viewColorIndicator.background.setTint(item.color)
    }

    override fun getItemCount() = items.size
}