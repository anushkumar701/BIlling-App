package com.fruitbilling.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fruitbilling.app.data.model.BillWithItems
import com.fruitbilling.app.data.repository.BillRepository
import com.fruitbilling.app.data.repository.MonthSalesSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

enum class HistoryTab(val title: String) {
    BILLS("Bills History"),
    SUMMARY("Business Summary")
}

data class HistoryUiState(
    val completedBills: List<BillWithItems> = emptyList(),
    val businessSummary: MonthSalesSummary = MonthSalesSummary(
        monthString = "",
        totalSales = BigDecimal.ZERO,
        totalBills = 0,
        cashSales = BigDecimal.ZERO,
        upiSales = BigDecimal.ZERO,
        avgBill = BigDecimal.ZERO,
        dailyBreakdown = emptyList()
    ),
    val selectedTab: HistoryTab = HistoryTab.BILLS,
    val selectedBillForDetail: BillWithItems? = null
)

class HistoryViewModel(
    private val billRepository: BillRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            billRepository.completedBills.collect { bills ->
                _uiState.value = _uiState.value.copy(completedBills = bills)
            }
        }

        viewModelScope.launch {
            billRepository.getBusinessSummary().collect { summary ->
                _uiState.value = _uiState.value.copy(businessSummary = summary)
            }
        }
    }

    fun onTabSelected(tab: HistoryTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun onBillSelected(bill: BillWithItems) {
        _uiState.value = _uiState.value.copy(selectedBillForDetail = bill)
    }

    fun onDismissDetail() {
        _uiState.value = _uiState.value.copy(selectedBillForDetail = null)
    }
}

class HistoryViewModelFactory(
    private val billRepository: BillRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            return HistoryViewModel(billRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
