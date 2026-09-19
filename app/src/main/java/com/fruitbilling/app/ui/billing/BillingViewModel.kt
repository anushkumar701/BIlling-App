package com.fruitbilling.app.ui.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fruitbilling.app.data.model.Bill
import com.fruitbilling.app.data.model.BillItem
import com.fruitbilling.app.data.model.BillWithItems
import com.fruitbilling.app.data.model.BillingMode
import com.fruitbilling.app.data.model.PaymentMethod
import com.fruitbilling.app.data.model.Product
import com.fruitbilling.app.data.model.ProductUnit
import com.fruitbilling.app.data.repository.BillRepository
import com.fruitbilling.app.data.repository.ProductRepository
import com.fruitbilling.app.data.repository.TodayStats
import com.fruitbilling.app.util.CalculatorEngine
import com.fruitbilling.app.util.MoneyUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

private val DEFAULT_WEIGHT_SHORTCUTS = listOf(
    "100g", "200g", "250g", "300g", "400g", "500g",
    "600g", "700g", "750g", "800g", "900g", "1kg"
)

data class BillingUiState(
    val currentBill: BillWithItems? = null,
    val activeAndHeldBills: List<BillWithItems> = emptyList(),
    val todayStats: TodayStats = TodayStats(),
    val billingMode: BillingMode = BillingMode.EXPERIENCED,
    val pendingExpression: String = "",
    val livePreview: String = "",        // live partial result shown in expression bar while typing
    val taggedProduct: Product? = null,
    val quickShortcuts: List<String> = DEFAULT_WEIGHT_SHORTCUTS,
    val searchQuery: String = "",
    val matchingProducts: List<Product> = emptyList(),
    val editingItem: BillItem? = null,
    val editInputText: String = "",
    val editCalculatedAmount: BigDecimal = BigDecimal.ZERO,
    val isEditInputValid: Boolean = false,
    val editErrorMessage: String? = null,
    val deletingItem: BillItem? = null,
    val lastDeletedItem: BillItem? = null,    // kept for 1-tap Undo in snackbar
    val finalPriceInput: String = "",
    val selectedPaymentMethod: PaymentMethod? = null,
    val isSaving: Boolean = false,
    val isSaveBillPromptOpen: Boolean = false
)

class BillingViewModel(
    private val productRepository: ProductRepository,
    private val billRepository: BillRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    private var allProductsCache: List<Product> = emptyList()

    // Tracks the Flow collector observing the currently-open bill so it can be
    // cancelled before a new one starts. Without this, switching/holding/creating
    // bills repeatedly leaves old collectors alive, which can overwrite the
    // current UI state with a stale bill's data the moment that old bill changes
    // in the database (state leakage between bills).
    private var currentBillObservationJob: Job? = null

    init {
        // Collect active products for search in Beginner Mode
        viewModelScope.launch {
            productRepository.activeProducts.collect { products ->
                allProductsCache = products
                updateMatchingProducts(_uiState.value.searchQuery)
            }
        }

        // Collect today's live stats
        viewModelScope.launch {
            billRepository.getTodayStats().collect { stats ->
                _uiState.value = _uiState.value.copy(todayStats = stats)
            }
        }

        // Collect active & held bills
        viewModelScope.launch {
            billRepository.activeAndHeldBills.collect { bills ->
                _uiState.value = _uiState.value.copy(activeAndHeldBills = bills)
            }
        }

        // Initialize active bill
        viewModelScope.launch {
            val initialBill = billRepository.getOrCreateActiveBill()
            observeCurrentBill(initialBill.bill.id)
        }
    }

    private fun observeCurrentBill(billId: Long) {
        // Cancel any previous bill's observer first -- otherwise it keeps running
        // in the background and will clobber uiState.currentBill with the old
        // bill's data whenever that old bill changes in the database.
        currentBillObservationJob?.cancel()
        currentBillObservationJob = viewModelScope.launch {
            billRepository.getBillWithItems(billId).collect { billWithItems ->
                if (billWithItems != null) {
                    // Room is the single source of truth for draft final price / payment
                    // (both are written immediately when the cashier sets them, see
                    // onFinalPriceChanged / onPaymentMethodSelected), so always take the
                    // DB value directly rather than falling back to in-memory state.
                    _uiState.value = _uiState.value.copy(
                        currentBill = billWithItems,
                        selectedPaymentMethod = billWithItems.bill.paymentMethod,
                        finalPriceInput = billWithItems.bill.finalAmount
                            ?.stripTrailingZeros()?.toPlainString()
                            ?: ""
                    )
                }
            }
        }
    }

    // --- Calculator Engine Input Handling (§3, §4, §6) ---

    fun onDigit(digit: String) {
        val current = _uiState.value.pendingExpression
        val newExpr = CalculatorEngine.appendDigit(current, digit)
        _uiState.value = _uiState.value.copy(pendingExpression = newExpr)
        updateLivePreview(newExpr)
    }

    fun onOperator(op: String) {
        val current = _uiState.value.pendingExpression.trim()
        if (current.isEmpty()) return

        // If ends with an operator, replace it
        val lastChar = current.last()
        val newExpr = if (lastChar in listOf('+', '\u2212', '-', '\u00D7', '*', '\u00F7', '/')) {
            current.dropLast(1) + " $op "
        } else {
            "$current $op "
        }
        _uiState.value = _uiState.value.copy(pendingExpression = newExpr)
        updateLivePreview(newExpr)
    }

    fun onDecimal() {
        val current = _uiState.value.pendingExpression
        val newExpr = CalculatorEngine.appendDecimal(current)
        if (newExpr != current) {
            _uiState.value = _uiState.value.copy(pendingExpression = newExpr)
            updateLivePreview(newExpr)
        }
    }

    fun onBackspace() {
        val current = _uiState.value.pendingExpression
        if (current.isEmpty()) return
        val newExpr = CalculatorEngine.applyBackspace(current)
        _uiState.value = _uiState.value.copy(pendingExpression = newExpr)
        updateLivePreview(newExpr)
    }

    fun onClearExpression() {
        _uiState.value = _uiState.value.copy(
            pendingExpression = "",
            livePreview = "",
            taggedProduct = null,
            quickShortcuts = DEFAULT_WEIGHT_SHORTCUTS
        )
    }

    /**
     * Recomputes the live preview of the current expression.
     * Shows the partial result in the expression bar so the cashier can
     * verify the math BEFORE pressing =.  Only shows a value when the
     * expression is complete enough to evaluate (no trailing operator).
     */
    private fun updateLivePreview(expr: String) {
        val trimmed = expr.trim()
        // Don't show preview if the expression ends with an operator (incomplete)
        val lastChar = trimmed.lastOrNull()
        if (lastChar == null || lastChar in listOf('+', '-', '×', '*', '÷', '/', '\u2212', '\u00D7', '\u00F7')) {
            _uiState.value = _uiState.value.copy(livePreview = "")
            return
        }
        val result = CalculatorEngine.evaluate(trimmed)
        _uiState.value = _uiState.value.copy(
            livePreview = result.getOrNull()
                ?.let { amount -> "= ${CalculatorEngine.formatDisplayAmount(amount)}" }
                ?: ""
        )
    }

    fun onShortcut(shortcut: String) {
        val current = _uiState.value.pendingExpression
        val priceStr = _uiState.value.taggedProduct?.price?.stripTrailingZeros()?.toPlainString()
        val toAppend = CalculatorEngine.applyShortcut(current, shortcut, priceStr)
        _uiState.value = _uiState.value.copy(pendingExpression = toAppend)
        updateLivePreview(toAppend)
    }

    fun onEquals() {
        val state = _uiState.value
        val expr = state.pendingExpression.trim()
        if (expr.isEmpty()) return

        val currentBill = state.currentBill ?: return

        val evalResult = CalculatorEngine.evaluate(expr)
        evalResult.onSuccess { amount ->
            viewModelScope.launch {
                val product = state.taggedProduct
                billRepository.addCalculation(
                    billId = currentBill.bill.id,
                    expression = expr,
                    amount = amount,
                    product = product
                )

                // Clear expression & product ready for next calculation (§7)
                _uiState.value = _uiState.value.copy(
                    pendingExpression = "",
                    livePreview = "",
                    taggedProduct = null,
                    quickShortcuts = DEFAULT_WEIGHT_SHORTCUTS
                )
            }
        }.onFailure { error ->
            viewModelScope.launch {
                _snackbarMessages.emit(error.message ?: "Invalid calculation")
            }
        }
    }

    // --- Beginner Mode Search & Tagging (§2 & §7) ---

    fun onToggleMode() {
        val nextMode = if (_uiState.value.billingMode == BillingMode.EXPERIENCED) {
            BillingMode.BEGINNER
        } else {
            BillingMode.EXPERIENCED
        }
        _uiState.value = _uiState.value.copy(billingMode = nextMode)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        updateMatchingProducts(query)
    }

    private fun updateMatchingProducts(query: String) {
        val trimmed = query.trim()
        val matched = if (trimmed.isEmpty()) {
            allProductsCache.filter { it.active }
        } else {
            allProductsCache.filter {
                it.active && it.name.contains(trimmed, ignoreCase = true)
            }
        }
        _uiState.value = _uiState.value.copy(matchingProducts = matched)
    }

    /**
     * Adds a product with specified quantity/weight directly to the active bill.
     * Core action for the Billing tab product picker.
     */
    fun addProductToBill(product: Product, quantityOrWeight: BigDecimal) {
        val currentBill = _uiState.value.currentBill ?: return
        if (quantityOrWeight <= BigDecimal.ZERO) return

        val unitPrice = product.price
        val amount = (unitPrice * quantityOrWeight).setScale(2, RoundingMode.HALF_UP)
        val qtyStr = if (product.unit == ProductUnit.KG) {
            CalculatorEngine.formatWeight(quantityOrWeight)
        } else {
            quantityOrWeight.stripTrailingZeros().toPlainString()
        }
        val priceStr = unitPrice.stripTrailingZeros().toPlainString()
        val expression = "$priceStr × $qtyStr"

        viewModelScope.launch {
            billRepository.addCalculation(
                billId = currentBill.bill.id,
                expression = expression,
                amount = amount,
                product = product,
                quantityOrWeight = quantityOrWeight,
                normalizedWeight = if (product.unit == ProductUnit.KG) quantityOrWeight else null
            )
            _snackbarMessages.emit("Added ${product.name} (₹$amount)")
        }
    }

    /**
     * Adds an arbitrary custom item or loose amount directly to the bill.
     */
    fun addCustomItemToBill(name: String, amount: BigDecimal) {
        val currentBill = _uiState.value.currentBill ?: return
        if (amount <= BigDecimal.ZERO) return

        val itemName = name.trim().ifEmpty { "Custom Item" }
        val expression = amount.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

        viewModelScope.launch {
            billRepository.addCalculation(
                billId = currentBill.bill.id,
                expression = "$itemName: ₹$expression",
                amount = amount.setScale(2, RoundingMode.HALF_UP),
                product = null
            )
            _snackbarMessages.emit("Added $itemName (₹$amount)")
        }
    }

    /**
     * Clears all items in the current draft bill.
     */
    fun onClearBill() {
        val currentBill = _uiState.value.currentBill ?: return
        viewModelScope.launch {
            billRepository.clearAllItems(currentBill.bill.id)
            _uiState.value = _uiState.value.copy(
                pendingExpression = "",
                livePreview = "",
                taggedProduct = null,
                finalPriceInput = "",
                selectedPaymentMethod = null
            )
            _snackbarMessages.emit("Bill items cleared")
        }
    }

    fun onProductSelected(product: Product) {
        // Attaches product name & price snapshot (§7)
        val priceStr = product.price.stripTrailingZeros().toPlainString()
        val shortcuts = if (product.unit == ProductUnit.PIECE) {
            listOf("1", "2", "3", "5", "10")
        } else {
            DEFAULT_WEIGHT_SHORTCUTS
        }

        _uiState.value = _uiState.value.copy(
            taggedProduct = product,
            pendingExpression = "$priceStr × ",
            quickShortcuts = shortcuts,
            searchQuery = "",
            matchingProducts = allProductsCache.filter { it.active }
        )
    }

    // --- Calculation List Actions: Edit & Delete (§5) ---

    fun onPromptDeleteItem(item: BillItem) {
        _uiState.value = _uiState.value.copy(deletingItem = item)
    }

    fun onCancelDeleteItem() {
        _uiState.value = _uiState.value.copy(deletingItem = null)
    }

    fun onConfirmDeleteItem() {
        val item = _uiState.value.deletingItem ?: return
        viewModelScope.launch {
            billRepository.removeCalculation(item)
            _uiState.value = _uiState.value.copy(
                deletingItem = null,
                lastDeletedItem = item   // stored so the UI can offer Undo
            )
            _snackbarMessages.emit("Deleted · tap Undo to restore")
        }
    }

    /** Restores the last deleted item — called from the Snackbar Undo action */
    fun onUndoDeleteItem() {
        val item = _uiState.value.lastDeletedItem ?: return
        _uiState.value = _uiState.value.copy(lastDeletedItem = null)
        viewModelScope.launch {
            billRepository.restoreItem(item)
        }
    }

    fun onPromptEditItem(item: BillItem) {
        val eval = CalculatorEngine.evaluate(item.expression).getOrDefault(item.calculatedAmount)
        _uiState.value = _uiState.value.copy(
            editingItem = item,
            editInputText = item.expression,
            editCalculatedAmount = eval,
            isEditInputValid = true,
            editErrorMessage = null
        )
    }

    fun onEditInputChanged(text: String) {
        val result = CalculatorEngine.evaluate(text)
        if (result.isSuccess) {
            _uiState.value = _uiState.value.copy(
                editInputText = text,
                editCalculatedAmount = result.getOrThrow(),
                isEditInputValid = true,
                editErrorMessage = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                editInputText = text,
                isEditInputValid = false,
                editErrorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun onConfirmEditItem() {
        val state = _uiState.value
        val item = state.editingItem ?: return
        if (!state.isEditInputValid) return

        viewModelScope.launch {
            billRepository.updateCalculation(
                item = item,
                newExpression = state.editInputText.trim(),
                newAmount = state.editCalculatedAmount
            )
            _uiState.value = _uiState.value.copy(
                editingItem = null,
                editInputText = "",
                editErrorMessage = null
            )
            _snackbarMessages.emit("Calculation updated")
        }
    }

    fun onDismissEditItem() {
        _uiState.value = _uiState.value.copy(
            editingItem = null,
            editInputText = "",
            editErrorMessage = null
        )
    }

    // --- Multiple Active Bills (§11) ---

    fun onNewBillTapped() {
        viewModelScope.launch {
            val newBill = billRepository.createNewBill()
            clearInputsForNewBill()
            observeCurrentBill(newBill.bill.id)
            _snackbarMessages.emit("Draft ${newBill.bill.formattedBillNumber} active")
        }
    }

    fun onHoldBillTapped() {
        val currentBill = _uiState.value.currentBill ?: return
        viewModelScope.launch {
            billRepository.holdBill(currentBill.bill.id)
            _snackbarMessages.emit("Bill ${currentBill.bill.formattedBillNumber} held")

            val nextBill = billRepository.getOrCreateActiveBill()
            clearInputsForNewBill()
            observeCurrentBill(nextBill.bill.id)
        }
    }

    fun onSwitchBill(billId: Long) {
        clearInputsForNewBill()
        viewModelScope.launch {
            billRepository.activateBill(billId)
            observeCurrentBill(billId)
        }
    }

    private fun clearInputsForNewBill() {
        _uiState.value = _uiState.value.copy(
            pendingExpression = "",
            taggedProduct = null,
            editingItem = null,
            deletingItem = null,
            finalPriceInput = "",
            selectedPaymentMethod = null,
            isSaving = false
        )
    }

    // --- Final Price & Payment (§9) ---

    fun onFinalPriceChanged(input: String) {
        _uiState.value = _uiState.value.copy(finalPriceInput = input)

        // Persist immediately so it survives app restart / process death (§12) --
        // this call only fires once per "Set"/"Clear" tap, not per keystroke.
        val billId = _uiState.value.currentBill?.bill?.id ?: return
        val trimmed = input.trim()
        val parsed = if (trimmed.isBlank()) null else trimmed.toBigDecimalOrNull()
        if (trimmed.isBlank() || parsed != null) {
            viewModelScope.launch {
                billRepository.updateDraftFinalAmount(billId, parsed)
            }
        }
    }

    fun onPaymentMethodSelected(method: PaymentMethod?) {
        _uiState.value = _uiState.value.copy(selectedPaymentMethod = method)

        // Persist immediately so it survives app restart / process death (§12).
        val billId = _uiState.value.currentBill?.bill?.id ?: return
        viewModelScope.launch {
            billRepository.updateDraftPaymentMethod(billId, method)
        }
    }

    // --- Save Bill Workflow (Human Cashier Review Prompt) ---

    fun onPromptSaveBill() {
        val currentBill = _uiState.value.currentBill ?: return
        if (currentBill.items.isEmpty()) {
            viewModelScope.launch {
                _snackbarMessages.emit("Bill cannot be empty. Add calculations or fruits first.")
            }
            return
        }
        _uiState.value = _uiState.value.copy(isSaveBillPromptOpen = true)
    }

    fun onDismissSaveBillPrompt() {
        _uiState.value = _uiState.value.copy(isSaveBillPromptOpen = false)
    }

    fun onConfirmSaveBill(finalAmountText: String, paymentMethod: PaymentMethod?) {
        val state = _uiState.value
        val currentBill = state.currentBill ?: return

        if (state.isSaving) return
        if (currentBill.items.isEmpty()) return

        val finalAmount = if (finalAmountText.isNotBlank()) {
            val parsed = finalAmountText.trim().toBigDecimalOrNull()
            if (parsed == null || parsed < BigDecimal.ZERO) {
                viewModelScope.launch { _snackbarMessages.emit("Invalid final price") }
                return
            }
            parsed
        } else null

        _uiState.value = _uiState.value.copy(isSaving = true, isSaveBillPromptOpen = false)

        viewModelScope.launch {
            val chosenMethod = paymentMethod ?: state.selectedPaymentMethod
            val result = billRepository.completeBill(
                billId = currentBill.bill.id,
                finalAmount = finalAmount,
                paymentMethod = chosenMethod
            )

            result.onSuccess { completedBill ->
                _uiState.value = _uiState.value.copy(isSaving = false)
                val payLabel = chosenMethod?.label ?: "Saved"
                _snackbarMessages.emit("Bill ${completedBill.formattedBillNumber} saved ($payLabel)")

                // Open clean next bill automatically (§10 & §12)
                val nextBill = billRepository.getOrCreateActiveBill()
                clearInputsForNewBill()
                observeCurrentBill(nextBill.bill.id)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isSaving = false)
                _snackbarMessages.emit("Save failed: ${error.message}. Bill is safe.")
            }
        }
    }

    fun onSaveBill() {
        onPromptSaveBill()
    }
}

class BillingViewModelFactory(
    private val productRepository: ProductRepository,
    private val billRepository: BillRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BillingViewModel::class.java)) {
            return BillingViewModel(productRepository, billRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
