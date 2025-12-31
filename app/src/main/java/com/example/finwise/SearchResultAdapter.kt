package com.example.finwise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.StockSearchResult

class SearchResultAdapter(
    private var stocks: List<StockSearchResult>,
    private val onItemClick: (StockSearchResult) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSymbol: TextView = view.findViewById(R.id.tvSymbol)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvExchange: TextView = view.findViewById(R.id.tvExchange)
        val ivLogo: ImageView = view.findViewById(R.id.ivLogo)
        val tvLogoInitials: TextView = view.findViewById(R.id.tvLogoInitials)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stock = stocks[position]
        
        holder.tvSymbol.text = stock.symbol
        holder.tvName.text = stock.name
        holder.tvExchange.text = stock.exchange ?: "NSE/BSE"
        
        // Show initials instead of logo
        holder.tvLogoInitials.text = stock.symbol.take(2).uppercase()
        holder.tvLogoInitials.visibility = View.VISIBLE
        holder.ivLogo.visibility = View.GONE
        
        holder.itemView.setOnClickListener { onItemClick(stock) }
    }

    override fun getItemCount() = stocks.size

    fun updateData(newStocks: List<StockSearchResult>) {
        stocks = newStocks
        notifyDataSetChanged()
    }

    fun clear() {
        stocks = emptyList()
        notifyDataSetChanged()
    }
}
