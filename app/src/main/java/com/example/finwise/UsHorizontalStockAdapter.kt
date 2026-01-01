package com.example.finwise

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.StockItem
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter for horizontal US Stock cards in Gainers/Losers sections.
 * Displays prices in USD ($).
 */
class UsHorizontalStockAdapter(
    private var stocks: List<StockItem>,
    private val onItemClick: (StockItem) -> Unit
) : RecyclerView.Adapter<UsHorizontalStockAdapter.ViewHolder>() {

    private val dollarFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLogoInitials: TextView = view.findViewById(R.id.tvLogoInitials)
        val tvSymbol: TextView = view.findViewById(R.id.tvSymbol)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvChange: TextView = view.findViewById(R.id.tvChange)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_us_stock_horizontal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stock = stocks[position]
        
        holder.tvLogoInitials.text = stock.logo_initial
        holder.tvSymbol.text = stock.symbol
        holder.tvPrice.text = dollarFormat.format(stock.price)
        
        // Format change
        val changeText = "${if (stock.is_positive) "+" else ""}${String.format("%.2f", stock.change_percent)}%"
        holder.tvChange.text = changeText
        holder.tvChange.setTextColor(Color.parseColor(if (stock.is_positive) "#22C55E" else "#EF4444"))
        
        holder.itemView.setOnClickListener { onItemClick(stock) }
    }

    override fun getItemCount() = stocks.size

    fun updateData(newStocks: List<StockItem>) {
        stocks = newStocks
        notifyDataSetChanged()
    }
}
