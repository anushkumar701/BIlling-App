package com.fruitbilling.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fruitbilling.app.data.model.BillWithItems
import com.fruitbilling.app.data.model.PaymentMethod
import com.fruitbilling.app.data.repository.BillRepository
import com.fruitbilling.app.data.repository.MonthSalesSummary
import com.fruitbilling.app.data.repository.TodayStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

enum class HistoryTab(val title: String) {
    BILLS("Bills History"),
    SUMMARY("Business Summary")
}

enum class PaymentFilter(val title: String) {
    ALL("All"),
    PENDING("⚠️ Pending"),
    CASH("Cash"),
    UPI("UPI")
}

data class HistoryUiState(
    val completedBills: List<BillWithItems> = emptyList(),
    val todayStats: TodayStats = TodayStats(),
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
    val selectedFilter: PaymentFilter = PaymentFilter.ALL,
    val searchQuery: String = "",
    val selectedBillForDetail: BillWithItems? = null
) {
    val filteredBills: List<BillWithItems>
        get() {
            val byPayment = when (selectedFilter) {
                PaymentFilter.ALL -> completedBills
                PaymentFilter.PENDING -> completedBills.filter { it.bill.paymentMethod == null }
                PaymentFilter.CASH -> completedBills.filter { it.bill.paymentMethod == PaymentMethod.CASH }
                PaymentFilter.UPI -> completedBills.filter { it.bill.paymentMethod == PaymentMethod.UPI }
            }
            if (searchQuery.isBlank()) return byPayment
            val q = searchQuery.trim().lowercase()
            return byPayment.filter { billWithItems ->
                billWithItems.bill.billNumber.toString().contains(q) ||
                billWithItems.bill.formattedBillNumber.lowercase().contains(q) ||
                (billWithItems.bill.finalAmount?.toPlainString()?.contains(q) == true) ||
                (billWithItems.bill.calculatedTotal.toPlainString().contains(q)) ||
                billWithItems.items.any { item ->
                    item.productNameSnapshot?.lowercase()?.contains(q) == true ||
                    item.expression.lowercase().contains(q)
                }
            }
        }

    val pendingCount: Int
        get() = completedBills.count { it.bill.paymentMethod == null }

    val cashCount: Int
        get() = completedBills.count { it.bill.paymentMethod == PaymentMethod.CASH }

    val upiCount: Int
        get() = completedBills.count { it.bill.paymentMethod == PaymentMethod.UPI }
}

class HistoryViewModel(
    private val billRepository: BillRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            billRepository.completedBills.collect { bills ->
                _uiState.value = _uiState.value.copy(completedBills = bills)
                // If a bill detail dialog is open, update its snapshot
                val currentDetail = _uiState.value.selectedBillForDetail
                if (currentDetail != null) {
                    val updated = bills.find { it.bill.id == currentDetail.bill.id }
                    if (updated != null) {
                        _uiState.value = _uiState.value.copy(selectedBillForDetail = updated)
                    }
                }
            }
        }

        viewModelScope.launch {
            billRepository.getTodayStats().collect { stats ->
                _uiState.value = _uiState.value.copy(todayStats = stats)
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

    fun onFilterSelected(filter: PaymentFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }


    fun onBillSelected(bill: BillWithItems) {
        _uiState.value = _uiState.value.copy(selectedBillForDetail = bill)
    }

    fun onDismissDetail() {
        _uiState.value = _uiState.value.copy(selectedBillForDetail = null)
    }

    /**
     * Updates payment method (Cash / UPI / null) on a completed bill.
     * Tapping the selected chip deselects it back to null (unspecified).
     */
    fun onUpdatePaymentMethod(billId: Long, method: PaymentMethod?) {
        viewModelScope.launch {
            billRepository.updateCompletedBillPaymentMethod(billId, method)
        }
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
