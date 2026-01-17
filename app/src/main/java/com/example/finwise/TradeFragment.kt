package com.example.finwise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TradeFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var fabQuickBuy: FloatingActionButton
    private lateinit var loadingOverlay: FrameLayout

    private var userEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trade, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)

        initializeViews(view)
        setupViewPager()
        setupListeners()
    }

    private fun initializeViews(view: View) {
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)
        fabQuickBuy = view.findViewById(R.id.fabQuickBuy)
        loadingOverlay = view.findViewById(R.id.loadingOverlay)
    }

    private fun setupViewPager() {
        // Create the pager adapter
        val pagerAdapter = TradePagerAdapter(this)
        viewPager.adapter = pagerAdapter
        
        // Enable user input (swipe gestures)
        viewPager.isUserInputEnabled = true
        
        // Set off-screen page limit for smoother transitions
        viewPager.offscreenPageLimit = 1

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Indian"
                1 -> "US"
                2 -> "Crypto"
                3 -> "Portfolio"
                else -> "Tab ${position + 1}"
            }
        }.attach()
    }

    private fun setupListeners() {
        fabQuickBuy.setOnClickListener { openBuyActivity(null) }
    }

    private fun openBuyActivity(prefilledSymbol: String?) {
        val intent = Intent(requireContext(), BuyAssetActivity::class.java).apply {
            putExtra("USER_EMAIL", userEmail)
            prefilledSymbol?.let { putExtra("PREFILLED_SYMBOL", it) }
        }
        startActivity(intent)
    }
}
