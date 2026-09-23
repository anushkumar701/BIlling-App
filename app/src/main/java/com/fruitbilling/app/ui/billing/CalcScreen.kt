package com.fruitbilling.app.ui.billing

import android.app.Activity
import android.view.WindowManager
import java.math.BigDecimal
import com.fruitbilling.app.util.MoneyUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * CalcScreen — the default landing tab (opens on app launch).
 *
 * Layout (top → bottom, ZERO wasted space):
 *  1. Compact header bar: bill# selector + stats + mode + hold (single row, no vertical padding)
 *  2. Scrollable bill-item list (weight=1f — fills ALL remaining space)
 *  3. Total + payment bar
 *  4. Expression bar: large input + live preview + product badge + CC button
 *  5. Calculator keypad
 *  6. Save Bill strip (full-width, below keypad — can't be confused with =)
 *
 * Key UX features:
 *  - Auto-dot: typing 0 or 1 after × auto-inserts "." (weight entry shortcut)
 *  - Live preview: "200 × 0.4" shows "= 80" before pressing =
 *  - Haptic on every keypad tap
 *  - Undo snackbar after item delete
 *  - Screen stays on while in the app (resets after 5 min idle)
 */
@Composable
fun CalcScreen(
    viewModel: BillingViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Keep screen on while billing; auto-resets after 5 min
    val context = LocalContext.current
    var keypadHeightDp by remember {
        mutableFloatStateOf(com.fruitbilling.app.data.preferences.CalcPreferences.getKeypadHeightDp(context))
    }
    LaunchedEffect(uiState) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        delay(5 * 60 * 1000L)
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    DisposableEffect(Unit) {
        onDispose {
            (context as? Activity)?.window
                ?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Snackbar with Undo support for item deletion
    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (message.startsWith("Deleted")) "Undo" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.onUndoDeleteItem()
            }
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
            // ── 1. Compact header (single row, no wasted space) ──────────────
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

            // ── 2. Bill item list (fills remaining space) ────────────────────
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

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // ── 3. Total + payment ───────────────────────────────────────────
            TotalAndPaymentBar(
                calculatedTotal = uiState.currentBill?.bill?.calculatedTotal
                    ?: java.math.BigDecimal.ZERO,
                finalPriceInput = uiState.finalPriceInput,
                selectedPaymentMethod = uiState.selectedPaymentMethod,
                onFinalPriceChanged = viewModel::onFinalPriceChanged,
                onPaymentMethodSelected = viewModel::onPaymentMethodSelected
            )

            // ── Adjustable Keypad Sizing Handle (S / M / L presets + drag) ───
            KeypadResizeHandle(
                currentHeightDp = keypadHeightDp,
                onHeightChanged = { newHeight ->
                    keypadHeightDp = newHeight
                    com.fruitbilling.app.data.preferences.CalcPreferences.setKeypadHeightDp(context, newHeight)
                }
            )

            // ── 4. Expression bar (big number + live preview + product badge) ─
            CalcExpressionBar(
                expression = uiState.pendingExpression,
                livePreview = uiState.livePreview,
                hasBillItems = uiState.currentBill?.items?.isNotEmpty() == true,
                taggedProductName = uiState.taggedProduct?.name,
                onClearAll = viewModel::onClearExpression,
                onClearBill = viewModel::onClearBill
            )

            // ── 5. Calculator keypad ─────────────────────────────────────────
            CalculatorKeypad(
                quickShortcuts = uiState.quickShortcuts,
                currentUnit = uiState.taggedProduct?.unit,
                onShortcutClicked = viewModel::onShortcut,
                onDigitClicked = viewModel::onDigit,
                onOperatorClicked = viewModel::onOperator,
                onDecimalClicked = viewModel::onDecimal,
                onEqualsClicked = viewModel::onEquals,
                onBackspace = viewModel::onBackspace,
                onClearAll = viewModel::onClearExpression,
                keyHeight = keypadHeightDp.dp
            )

            // ── 6. Save Bill strip ───────────────────────────────────────────
            CalcSaveStrip(
                isSaving = uiState.isSaving,
                totalAmount = uiState.currentBill?.bill?.calculatedTotal,
                onSaveClicked = viewModel::onPromptSaveBill
            )
        }

        // Dialogs (rendered over content)
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

        uiState.deletingItem?.let { item ->
            DeleteCalculationDialog(
                item = item,
                onConfirmDelete = viewModel::onConfirmDeleteItem,
                onDismiss = viewModel::onCancelDeleteItem
            )
        }

        if (uiState.isSaveBillPromptOpen) {
            uiState.currentBill?.let { bill ->
                SaveBillDialog(
                    billWithItems = bill,
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
}

// ─────────────────────────────────────────────────────────────────────────────
// Expression bar
//
// Layout:
//   [ Product badge (if any) ]       [CC]
//   [ Big expression number  ]
//   [ Live preview = 120     ]  ← ghost text, green, shows result before =
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CalcExpressionBar(
    expression: String,
    livePreview: String,
    hasBillItems: Boolean,
    taggedProductName: String?,
    onClearAll: () -> Unit,
    onClearBill: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearAllDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Product badge row
                if (!taggedProductName.isNullOrBlank()) {
                    Text(
                        text = "▸ $taggedProductName",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Main expression — large, full-width, easy to read while talking
                Text(
                    text = expression.ifEmpty { "0" },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (expression.isEmpty())
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onSurface,
                        fontSize = 30.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Live preview — shows "= 80" before pressing =
                if (livePreview.isNotEmpty()) {
                    Text(
                        text = livePreview,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            // CC (clear current entry or clear whole bill with prompt)
            if (expression.isNotEmpty() || hasBillItems) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    onClick = { showClearAllDialog = true },
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
                    modifier = Modifier.height(34.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = "CC",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }
            }
        }
    }

    if (showClearAllDialog) {
        val isClearingBill = expression.isEmpty() && hasBillItems
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = if (isClearingBill) "Clear Entire Bill?" else "Clear Current Entry?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isClearingBill)
                        "This will remove all items from the current bill. This cannot be undone."
                    else
                        "Clears what you're typing.\nBill items already saved are NOT affected."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllDialog = false
                        if (isClearingBill) onClearBill() else onClearAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Save Bill strip — well below keypad, impossible to fat-finger with =
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CalcSaveStrip(
    isSaving: Boolean,
    onSaveClicked: () -> Unit,
    modifier: Modifier = Modifier,
    totalAmount: BigDecimal? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Button(
            onClick = onSaveClicked,
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 3.dp, pressedElevation = 6.dp
            )
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Saving…", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } else {
                val label = if (totalAmount != null && totalAmount > BigDecimal.ZERO) {
                    "💾  Save Bill (${MoneyUtils.formatPrice(totalAmount)})"
                } else {
                    "💾  Save Bill"
                }
                Text(
                    text = label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun KeypadResizeHandle(
    currentHeightDp: Float,
    onHeightChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    // Dragging up increases keypad height, dragging down decreases keypad height
                    val newHeight = currentHeightDp - (dragAmount / 2.5f)
                    onHeightChanged(newHeight.coerceIn(38f, 65f))
                }
            }
            .padding(vertical = 3.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Preset chips: S (Compact), M (Standard), L (Large Rush)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "S (Compact)" to com.fruitbilling.app.data.preferences.CalcPreferences.COMPACT_HEIGHT_DP,
                "M" to com.fruitbilling.app.data.preferences.CalcPreferences.DEFAULT_HEIGHT_DP,
                "L (Rush)" to com.fruitbilling.app.data.preferences.CalcPreferences.LARGE_HEIGHT_DP
            ).forEach { (label, presetHeight) ->
                val isSelected = kotlin.math.abs(currentHeightDp - presetHeight) < 4f
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable { onHeightChanged(presetHeight) }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Center drag pill
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        )

        Text(
            text = "${currentHeightDp.toInt()}dp",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

