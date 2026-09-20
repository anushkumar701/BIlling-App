package com.fruitbilling.app.ui.billing

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fruitbilling.app.data.model.Product
import kotlinx.coroutines.delay

/**
 * BillingScreen — "Billing" tab: Search fruits and build bill.
 *
 * Full cashier workflow:
 *  1. Header: bill number dropdown (#001 ▾), Hold, today's sales
 *  2. Fruit Catalog & Search:
 *     - Real-time search by fruit name
 *     - 2-row compact horizontal chips showing fruit emoji, name, price
 *     - 1-tap on fruit opens AddProductDialog with quick weight presets (250g, 500g, 1kg) + custom weight
 *     - "+ Custom Item" button for loose/arbitrary items
 *  3. Current Bill List:
 *     - Real-time items list with unit, price, and calculated amount
 *     - Tap-to-edit and tap-to-delete with confirmation and undo
 *  4. Total & Payment bar (Cash / UPI / Set Final Price)
 *  5. Full-width prominent SAVE BILL button with live total
 */
@Composable
fun BillingScreen(
    viewModel: BillingViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedProductForAdd by remember { mutableStateOf<Product?>(null) }
    var showCustomItemDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(uiState) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        delay(5 * 60 * 1000L)
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    DisposableEffect(Unit) {
        onDispose {
            val window = (context as? Activity)?.window
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Header: bill selector, hold, today stats
            BillingHeader(
                currentBill = uiState.currentBill,
                activeAndHeldBills = uiState.activeAndHeldBills,
                billingMode = uiState.billingMode,
                todayStats = uiState.todayStats,
                onHoldBill = viewModel::onHoldBillTapped,
                onSwitchBill = viewModel::onSwitchBill,
                onNewBill = viewModel::onNewBillTapped,
                onToggleMode = viewModel::onToggleMode
            )

            // 2. Fruit Catalog with Search & Quick Fruit Chips
            ProductCatalogSection(
                products = uiState.matchingProducts,
                searchQuery = uiState.searchQuery,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onProductClicked = { product -> selectedProductForAdd = product },
                onCustomItemClicked = { showCustomItemDialog = true }
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            // 3. Current Bill items list
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                CurrentBillSection(
                    billWithItems = uiState.currentBill,
                    onEditItem = viewModel::onPromptEditItem,
                    onDeleteItem = viewModel::onPromptDeleteItem,
                    modifier = Modifier.fillMaxSize()
                )
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            // 4. Total + payment selector
            TotalAndPaymentBar(
                calculatedTotal = uiState.currentBill?.bill?.calculatedTotal ?: java.math.BigDecimal.ZERO,
                finalPriceInput = uiState.finalPriceInput,
                selectedPaymentMethod = uiState.selectedPaymentMethod,
                onFinalPriceChanged = viewModel::onFinalPriceChanged,
                onPaymentMethodSelected = viewModel::onPaymentMethodSelected
            )

            // 5. 1-Tap Save Bill button
            CalcSaveStrip(
                isSaving = uiState.isSaving,
                totalAmount = uiState.currentBill?.bill?.calculatedTotal,
                onSaveClicked = viewModel::onPromptSaveBill
            )
        }

        // Dialog: Add Fruit to Bill (with quick presets & custom weight)
        selectedProductForAdd?.let { product ->
            AddProductDialog(
                product = product,
                onConfirm = { quantity ->
                    viewModel.addProductToBill(product, quantity)
                    selectedProductForAdd = null
                },
                onDismiss = { selectedProductForAdd = null }
            )
        }

        // Dialog: Add Custom / Non-catalog Item
        if (showCustomItemDialog) {
            AddCustomItemDialog(
                onConfirm = { name, amount ->
                    viewModel.addCustomItemToBill(name, amount)
                    showCustomItemDialog = false
                },
                onDismiss = { showCustomItemDialog = false }
            )
        }

        // Dialog: Edit Item
        uiState.editingItem?.let { item ->
            EditItemDialog(
                item = item,
                inputText = uiState.editInputText,
                calculatedAmount = uiState.editCalculatedAmount,
                isValid = uiState.isEditInputValid,
                errorMessage = uiState.editErrorMessage,
                onInputChanged = viewModel::onEditInputChanged,
                onConfirmEdit = viewModel::onConfirmEditItem,
                onDismiss = viewModel::onDismissEditItem
            )
        }

        // Dialog: Delete Item confirmation
        uiState.deletingItem?.let { item ->
            DeleteCalculationDialog(
                item = item,
                onConfirmDelete = viewModel::onConfirmDeleteItem,
                onDismiss = viewModel::onCancelDeleteItem
            )
        }

        // Dialog: Save Bill Review Prompt
        if (uiState.isSaveBillPromptOpen && uiState.currentBill != null) {
            SaveBillDialog(
                billWithItems = uiState.currentBill!!,
                initialFinalPrice = uiState.finalPriceInput,
                initialPaymentMethod = uiState.selectedPaymentMethod,
                isSaving = uiState.isSaving,
                onConfirmSave = viewModel::onConfirmSaveBill,
                onDismiss = viewModel::onDismissSaveBillPrompt,
                onDeleteItem = viewModel::onDirectDeleteItem
            )
        }
    }
}
