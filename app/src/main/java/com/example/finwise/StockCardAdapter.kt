package com.example.finwise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.StockItem
import java.text.NumberFormat
import java.util.Locale

class StockCardAdapter(
    private var stocks: List<StockItem>,
    private val onClick: (StockItem) -> Unit
) : RecyclerView.Adapter<StockCardAdapter.ViewHolder>() {

    private val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

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
        val context = holder.itemView.context

        holder.tvLogoInitial.text = stock.logo_initial
        holder.tvStockName.text = stock.name
        holder.tvStockSector.text = stock.sector ?: "Stock"
        holder.tvStockPrice.text = rupeeFormat.format(stock.price)

        // Set change text with +/- sign (no arrow)
        val sign = if (stock.is_positive) "+" else "-"
        val changeText = "$sign${String.format("%.2f", kotlin.math.abs(stock.change_percent))}%"
        holder.tvStockChange.text = changeText
        
        // Apply appropriate text color based on positive/negative
        holder.tvStockChange.setTextColor(
            ContextCompat.getColor(
                context,
                if (stock.is_positive) R.color.pill_positive_text else R.color.pill_negative_text
            )
        )

        holder.itemView.setOnClickListener { onClick(stock) }
    }

    override fun getItemCount() = stocks.size

    fun updateData(newStocks: List<StockItem>) {
        stocks = newStocks
        notifyDataSetChanged()
    }
}
