package com.example.finwise

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    // We have 4 swipeable tabs: Home, Budget, Learn, Profile
    // (The middle "Add" button in the menu is skipped in the pager)
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        // Position 0 = Home
        // Position 1 = Budget
        // Position 2 = Learn
        // Position 3 = Profile
        return when (position) {
            0 -> DashboardFragment()
            1 -> BudgetFragment()
            2 -> LearnFragment()
            3 -> ProfileFragment()
            else -> DashboardFragment() // Should not happen
        }
    }
}