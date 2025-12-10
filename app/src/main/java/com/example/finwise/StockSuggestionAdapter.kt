package com.example.finwise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.StockSearchResult

class StockSuggestionAdapter(
    private var suggestions: List<StockSearchResult>,
    private val onItemClick: (StockSearchResult) -> Unit
) : RecyclerView.Adapter<StockSuggestionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStockIcon: TextView = view.findViewById(R.id.tvStockIcon)
        val tvStockName: TextView = view.findViewById(R.id.tvStockName)
        val tvStockSymbol: TextView = view.findViewById(R.id.tvStockSymbol)
        val tvExchange: TextView = view.findViewById(R.id.tvExchange)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stock_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stock = suggestions[position]
        
        // Show first letter as icon
        holder.tvStockIcon.text = stock.symbol.firstOrNull()?.toString() ?: "?"
        holder.tvStockName.text = stock.name
        holder.tvStockSymbol.text = stock.symbol
        holder.tvExchange.text = stock.exchange ?: "Stock"
        
        // Set exchange badge color
        when (stock.exchange) {
            "NSE/BSE" -> {
                holder.tvExchange.setBackgroundResource(R.drawable.bg_exchange_badge)
                holder.tvExchange.setTextColor(holder.itemView.context.getColor(R.color.finwise_green))
            }
            "US" -> {
                holder.tvExchange.setBackgroundResource(R.drawable.bg_exchange_badge_us)
                holder.tvExchange.setTextColor(holder.itemView.context.getColor(R.color.blue_primary))
            }
            "Crypto" -> {
                holder.tvExchange.setBackgroundResource(R.drawable.bg_exchange_badge_crypto)
                holder.tvExchange.setTextColor(holder.itemView.context.getColor(R.color.orange_crypto))
            }
            else -> {
                holder.tvExchange.setBackgroundResource(R.drawable.bg_exchange_badge)
                holder.tvExchange.setTextColor(holder.itemView.context.getColor(R.color.finwise_green))
            }
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(stock)
        }
    }

    override fun getItemCount(): Int = suggestions.size

    fun updateData(newSuggestions: List<StockSearchResult>) {
        suggestions = newSuggestions
        notifyDataSetChanged()
    }
    
    fun clear() {
        suggestions = emptyList()
        notifyDataSetChanged()
    }
}
