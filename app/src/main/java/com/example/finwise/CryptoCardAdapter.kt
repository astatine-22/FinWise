package com.example.finwise

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.CryptoItem
import java.text.NumberFormat
import java.util.Locale

class CryptoCardAdapter(
    private var cryptos: List<CryptoItem>,
    private val onClick: (CryptoItem) -> Unit
) : RecyclerView.Adapter<CryptoCardAdapter.ViewHolder>() {

    private val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    // Crypto color palette
    private val colors = listOf(
        "#F7931A", "#627EEA", "#00D4AA", "#23292F",
        "#C3A634", "#0033AD", "#E84142", "#E6007A"
    )

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCryptoInitial: TextView = view.findViewById(R.id.tvCryptoInitial)
        val tvCryptoName: TextView = view.findViewById(R.id.tvCryptoName)
        val tvCryptoShortName: TextView = view.findViewById(R.id.tvCryptoShortName)
        val tvCryptoPrice: TextView = view.findViewById(R.id.tvCryptoPrice)
        val tvCryptoChange: TextView = view.findViewById(R.id.tvCryptoChange)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_crypto_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val crypto = cryptos[position]

        holder.tvCryptoInitial.text = crypto.logo_initial
        holder.tvCryptoName.text = crypto.name
        holder.tvCryptoShortName.text = crypto.short_name
        holder.tvCryptoPrice.text = rupeeFormat.format(crypto.price_inr)

        // Set change text with arrow
        val arrow = if (crypto.is_positive) "▲" else "▼"
        val changeText = "$arrow ${String.format("%.2f", kotlin.math.abs(crypto.change_percent_24h))}%"
        holder.tvCryptoChange.text = changeText
        holder.tvCryptoChange.setTextColor(
            if (crypto.is_positive) Color.parseColor("#4CAF50") 
            else Color.parseColor("#F44336")
        )

        // Set logo background color via parent FrameLayout
        val logoContainer = holder.tvCryptoInitial.parent as? FrameLayout
        logoContainer?.background?.setTint(Color.parseColor(colors[position % colors.size]))

        holder.itemView.setOnClickListener { onClick(crypto) }
    }

    override fun getItemCount() = cryptos.size

    fun updateData(newCryptos: List<CryptoItem>) {
        cryptos = newCryptos
        notifyDataSetChanged()
    }
}
