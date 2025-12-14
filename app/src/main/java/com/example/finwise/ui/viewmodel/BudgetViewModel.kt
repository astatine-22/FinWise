package com.example.finwise.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.finwise.data.local.entity.LocalExpense
import com.example.finwise.data.repository.ExpenseRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Budget/Expense screen.
 * Demonstrates the Offline-First architecture usage:
 * 1. Observes data from Repository (Flow -> LiveData)
 * 2. Triggers data refresh in background (API -> DB)
 */
class BudgetViewModel(
    private val expenseRepository: ExpenseRepository,
    private val userEmail: String
) : ViewModel() {

    /**
     * Source of Truth: Data comes directly from the local database via the Repository.
     * We convert Flow to LiveData for easy observation in the Activity/Fragment.
     */
    val allExpenses: LiveData<List<LocalExpense>> = expenseRepository.allExpenses.asLiveData()

    init {
        // Trigger a background refresh when the ViewModel is created
        refreshData()
    }

    /**
     * Refresh data from the API.
     * The UI doesn't need to observe the result of this directly,
     * because the 'allExpenses' LiveData will automatically update
     * when the repository saves the new data to the database.
     */
    fun refreshData() {
        viewModelScope.launch {
            expenseRepository.refreshExpenses(userEmail)
        }
    }
}

/**
 * Manual Dependency Injection Factory.
 * (In a real app, use Hilt or Koin)
 */
class BudgetViewModelFactory(
    private val repository: ExpenseRepository,
    private val email: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgetViewModel(repository, email) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
