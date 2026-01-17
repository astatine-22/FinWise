package com.example.finwise

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class TradePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> IndianMarketFragment()
            1 -> UsMarketFragment()
            2 -> CryptoMarketFragment()
            3 -> PortfolioFragment()
            else -> IndianMarketFragment()
        }
    }
}
