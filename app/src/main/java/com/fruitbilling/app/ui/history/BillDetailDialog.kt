package com.fruitbilling.app.ui.history

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitbilling.app.data.model.BillWithItems
import com.fruitbilling.app.data.model.PaymentMethod
import com.fruitbilling.app.ui.theme.CashGreen
import com.fruitbilling.app.ui.theme.UpiBlue
import com.fruitbilling.app.util.DateUtils
import com.fruitbilling.app.util.MoneyUtils

@Composable
fun BillDetailDialog(
    billWithItems: BillWithItems,
    onUpdatePaymentMethod: (PaymentMethod?) -> Unit,
    onDeleteBill: ((Long) -> Unit)? = null,
    onEditBill: ((BillWithItems) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val bill = billWithItems.bill
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    fun shareReceipt() {
        val shopName = com.fruitbilling.app.data.preferences.ShopPreferences.getShopName(context)
        val shopPhone = com.fruitbilling.app.data.preferences.ShopPreferences.getShopPhone(context)
        val receiptText = com.fruitbilling.app.util.ReceiptUtils.generateReceiptText(
            billWithItems = billWithItems,
            storeName = shopName,
            storePhone = shopPhone
        )
        com.fruitbilling.app.util.ReceiptUtils.shareReceipt(context, receiptText, "Share Receipt - ${bill.formattedBillNumber}")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bill ${bill.formattedBillNumber}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                val isUpi = bill.paymentMethod == PaymentMethod.UPI
                val isCash = bill.paymentMethod == PaymentMethod.CASH
                Surface(
                    color = when {
                        isUpi -> UpiBlue.copy(alpha = 0.15f)
                        isCash -> CashGreen.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = bill.paymentMethod?.label ?: "⚠️ Unspecified",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isUpi -> UpiBlue
                                isCash -> CashGreen
                                else -> MaterialTheme.colorScheme.outline
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Timestamp
                bill.completedAt?.let { completedAt ->
                    Text(
                        text = DateUtils.formatDetailedTimestamp(completedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Calculations list
                Text(
                    text = "Calculations (${billWithItems.items.size}):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    billWithItems.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.displayExpression,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                if (item.unitPriceSnapshot != null && item.unit != null) {
                                    Text(
                                        text = "${item.formattedQuantityOrWeight ?: ""} @ ${MoneyUtils.formatWholePrice(item.unitPriceSnapshot)}${item.unit.unitLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Text(
                                text = MoneyUtils.formatPrice(item.calculatedAmount),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // Calculated total & Final charged
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Calculated Total:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = MoneyUtils.formatPrice(bill.calculatedTotal),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                if (bill.finalAmount != null && bill.finalAmount.compareTo(bill.calculatedTotal) != 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Adjusted Final Price:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = MoneyUtils.formatPrice(bill.finalAmount),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TOTAL PAID:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = MoneyUtils.formatPrice(bill.effectiveChargedAmount),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // ── Interactive Payment Mode Switcher ──────────────────────────
                // Cashiers can set or change to Cash or UPI anytime.
                // Tapping the selected option deselects back to unspecified.
                // No explicit "Unspecified" button exists.
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Change Payment Mode:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailPaymentChip(
                            label = "💵 Cash",
                            isSelected = bill.paymentMethod == PaymentMethod.CASH,
                            activeColor = CashGreen,
                            onClick = {
                                if (bill.paymentMethod == PaymentMethod.CASH) {
                                    onUpdatePaymentMethod(null) // deselect
                                } else {
                                    onUpdatePaymentMethod(PaymentMethod.CASH)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        DetailPaymentChip(
                            label = "📱 UPI",
                            isSelected = bill.paymentMethod == PaymentMethod.UPI,
                            activeColor = UpiBlue,
                            onClick = {
                                if (bill.paymentMethod == PaymentMethod.UPI) {
                                    onUpdatePaymentMethod(null) // deselect
                                } else {
                                    onUpdatePaymentMethod(PaymentMethod.UPI)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDeleteBill != null) {
                    TextButton(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("🗑️ Delete", fontWeight = FontWeight.SemiBold)
                    }
                }
                if (onEditBill != null) {
                    TextButton(onClick = { onEditBill(billWithItems) }) {
                        Text("✏️ Edit", fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = ::shareReceipt) {
                    Text("📤 Share", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    )

    if (showDeleteConfirmDialog && onDeleteBill != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete Bill ${bill.formattedBillNumber}?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text("Are you sure you want to delete this bill? This action cannot be undone and will remove it from sales history.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteBill(bill.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Bill", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailPaymentChip(
    label: String,
    isSelected: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) BorderStroke(1.5.dp, activeColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
