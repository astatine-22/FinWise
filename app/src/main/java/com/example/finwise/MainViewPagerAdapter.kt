package com.example.finwise

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    // List of fragments for the main ViewPager
    // Order: Home (Dashboard), Budget, Learn, Trade
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DashboardFragment()
            1 -> BudgetFragment()
            2 -> LearnFragment()
            3 -> TradeFragment()
            else -> DashboardFragment()
        }
    }
}
