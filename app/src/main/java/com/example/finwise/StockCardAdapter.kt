package com.example.finwise

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.StockItem
import java.text.NumberFormat
import java.util.Locale

class StockCardAdapter(
    private var stocks: List<StockItem>,
    private val onClick: (StockItem) -> Unit
) : RecyclerView.Adapter<StockCardAdapter.ViewHolder>() {

    private val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    // Color palette for stock initials
    private val colors = listOf(
        "#4CAF50", "#2196F3", "#9C27B0", "#FF5722", 
        "#00BCD4", "#E91E63", "#3F51B5", "#FF9800"
    )

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLogoInitial: TextView = view.findViewById(R.id.tvLogoInitial)
        val tvStockName: TextView = view.findViewById(R.id.tvStockName)
        val tvStockSector: TextView = view.findViewById(R.id.tvStockSector)
        val tvStockPrice: TextView = view.findViewById(R.id.tvStockPrice)
        val tvStockChange: TextView = view.findViewById(R.id.tvStockChange)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stock_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stock = stocks[position]

        holder.tvLogoInitial.text = stock.logo_initial
        holder.tvStockName.text = stock.name
        holder.tvStockSector.text = stock.sector ?: "Stock"
        holder.tvStockPrice.text = rupeeFormat.format(stock.price)

        // Set change text with arrow
        val arrow = if (stock.is_positive) "▲" else "▼"
        val changeText = "$arrow ${String.format("%.2f", kotlin.math.abs(stock.change_percent))}%"
        holder.tvStockChange.text = changeText
        holder.tvStockChange.setTextColor(
            if (stock.is_positive) Color.parseColor("#4CAF50") 
            else Color.parseColor("#F44336")
        )

        // Set logo background color via parent FrameLayout
        val logoContainer = holder.tvLogoInitial.parent as? FrameLayout
        logoContainer?.background?.setTint(Color.parseColor(colors[position % colors.size]))

        holder.itemView.setOnClickListener { onClick(stock) }
    }

    override fun getItemCount() = stocks.size

    fun updateData(newStocks: List<StockItem>) {
        stocks = newStocks
        notifyDataSetChanged()
    }
}
