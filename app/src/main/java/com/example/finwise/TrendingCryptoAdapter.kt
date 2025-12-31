package com.example.finwise

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.CryptoItem

class TrendingCryptoAdapter(
    private var cryptos: List<CryptoItem>,
    private val onItemClick: (CryptoItem) -> Unit
) : RecyclerView.Adapter<TrendingCryptoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLogoInitials: TextView = view.findViewById(R.id.tvLogoInitials)
        val tvSymbol: TextView = view.findViewById(R.id.tvSymbol)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvChange: TextView = view.findViewById(R.id.tvChange)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trending_stock, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val crypto = cryptos[position]
        
        holder.tvLogoInitials.text = crypto.logo_initial
        holder.tvSymbol.text = crypto.short_name
        holder.tvPrice.text = "$${String.format("%.2f", crypto.price_usd)}"
        
        // Format change
        val changeText = "${if (crypto.is_positive) "+" else ""}${String.format("%.2f", crypto.change_percent_24h)}%"
        holder.tvChange.text = changeText
        holder.tvChange.setTextColor(Color.parseColor(if (crypto.is_positive) "#22C55E" else "#EF4444"))
        
        holder.itemView.setOnClickListener { onItemClick(crypto) }
    }

    override fun getItemCount() = cryptos.size

    fun updateData(newCryptos: List<CryptoItem>) {
        cryptos = newCryptos
        notifyDataSetChanged()
    }
}
