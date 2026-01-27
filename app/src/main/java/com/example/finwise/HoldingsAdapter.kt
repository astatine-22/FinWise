package com.example.finwise

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.HoldingResponse
import java.text.NumberFormat

class HoldingsAdapter(
    private var holdings: List<HoldingResponse>,
    private val rupeeFormat: NumberFormat,
    private val onItemClick: (HoldingResponse) -> Unit
) : RecyclerView.Adapter<HoldingsAdapter.HoldingViewHolder>() {

    class HoldingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSymbolIcon: TextView = itemView.findViewById(R.id.tvSymbolIcon)
        val tvSymbol: TextView = itemView.findViewById(R.id.tvSymbol)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val tvAvgPrice: TextView = itemView.findViewById(R.id.tvAvgPrice)
        val tvCurrentValue: TextView = itemView.findViewById(R.id.tvCurrentValue)
        val tvProfitLoss: TextView = itemView.findViewById(R.id.tvProfitLoss)
        val tvProfitLossPercent: TextView = itemView.findViewById(R.id.tvProfitLossPercent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HoldingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_holding, parent, false)
        return HoldingViewHolder(view)
    }

    override fun onBindViewHolder(holder: HoldingViewHolder, position: Int) {
        val holding = holdings[position]

        // Set symbol icon (first letter)
        val symbolName = holding.asset_symbol.replace(".NS", "").replace(".BO", "")
        holder.tvSymbolIcon.text = symbolName.firstOrNull()?.toString() ?: "?"

        // Set symbol and details
        holder.tvSymbol.text = holding.asset_symbol
        
        // Format quantity
        val quantityText = if (holding.quantity == holding.quantity.toLong().toFloat()) {
            "${holding.quantity.toLong()} shares"
        } else {
            String.format("%.4f shares", holding.quantity)
        }
        holder.tvQuantity.text = quantityText
        
        // Average price
        holder.tvAvgPrice.text = "Avg: ${rupeeFormat.format(holding.average_buy_price)}"

        // Current value
        val currentValue = holding.current_value ?: (holding.average_buy_price * holding.quantity)
        holder.tvCurrentValue.text = rupeeFormat.format(currentValue)

        // Profit/Loss
        val profitLoss = holding.profit_loss ?: 0f
        val profitLossPercent = holding.profit_loss_percent ?: 0f

        val isProfit = profitLoss >= 0
        val profitColor = if (isProfit) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")

        val profitSign = if (isProfit) "+" else ""
        holder.tvProfitLoss.text = "$profitSign${rupeeFormat.format(profitLoss)}"
        holder.tvProfitLoss.setTextColor(profitColor)

        holder.tvProfitLossPercent.text = " ($profitSign${String.format("%.2f", profitLossPercent)}%)"
        holder.tvProfitLossPercent.setTextColor(profitColor)

        // Set click listener on root view
        holder.itemView.setOnClickListener {
            onItemClick(holding)
        }
    }

    override fun getItemCount(): Int = holdings.size

    fun updateData(newHoldings: List<HoldingResponse>) {
        holdings = newHoldings
        notifyDataSetChanged()
    }
}
