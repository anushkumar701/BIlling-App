package com.fruitbilling.app.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitbilling.app.data.model.BillItem
import com.fruitbilling.app.data.model.BillWithItems
import com.fruitbilling.app.data.model.PaymentMethod
import com.fruitbilling.app.data.model.ProductUnit
import com.fruitbilling.app.ui.theme.CashGreen
import com.fruitbilling.app.ui.theme.PendingOrange
import com.fruitbilling.app.ui.theme.UpiBlue
import com.fruitbilling.app.util.CalculatorEngine
import com.fruitbilling.app.util.MoneyUtils
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Full-screen-style dialog for editing a completed bill.
 * Allows: editing item quantities, removing items, adding new freeform items,
 * changing payment method, adjusting final price, and updating customer name.
 * The bill number (#105) stays unchanged.
 */
@Composable
fun EditCompletedBillDialog(
    billWithItems: BillWithItems,
    onSave: (billId: Long, items: List<BillItem>, finalAmount: BigDecimal?, paymentMethod: PaymentMethod?, customerName: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val bill = billWithItems.bill
    val editableItems = remember { mutableStateListOf(*billWithItems.items.toTypedArray()) }
    var selectedPayment by remember { mutableStateOf(bill.paymentMethod) }
    var customerNameInput by remember { mutableStateOf(bill.customerName ?: "") }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf(-1) }
    var editingItemQtyText by remember { mutableStateOf("") }

    // Compute running total from editable items
    val calculatedTotal = editableItems.fold(BigDecimal.ZERO) { acc, item ->
        acc.add(item.calculatedAmount)
    }.setScale(2, RoundingMode.HALF_UP)

    var finalPriceText by remember {
        mutableStateOf(
            bill.finalAmount?.stripTrailingZeros()?.toPlainString()
                ?: calculatedTotal.stripTrailingZeros().toPlainString()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Edit Bill ${bill.formattedBillNumber}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Items List ────────────────────────────────────────────────
                Text(
                    text = "Items (${editableItems.size}):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                editableItems.forEachIndexed { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        if (editingItemIndex == index) {
                            // Inline edit mode for this item
                            EditItemInline(
                                item = item,
                                qtyText = editingItemQtyText,
                                onQtyChange = { editingItemQtyText = it },
                                onConfirm = {
                                    val newQty = editingItemQtyText.trim().toBigDecimalOrNull()
                                    if (newQty != null && newQty > BigDecimal.ZERO && item.unitPriceSnapshot != null) {
                                        val newAmount = (item.unitPriceSnapshot * newQty).setScale(2, RoundingMode.HALF_UP)
                                        val qtyStr = if (item.unit == ProductUnit.KG) {
                                            CalculatorEngine.formatWeight(newQty)
                                        } else {
                                            newQty.stripTrailingZeros().toPlainString()
                                        }
                                        val priceStr = item.unitPriceSnapshot.stripTrailingZeros().toPlainString()
                                        val newExpr = "$priceStr × $qtyStr"
                                        editableItems[index] = item.copy(
                                            expression = newExpr,
                                            calculatedAmount = newAmount,
                                            quantityOrWeight = newQty,
                                            normalizedWeight = if (item.unit == ProductUnit.KG) newQty else null
                                        )
                                    }
                                    editingItemIndex = -1
                                },
                                onCancel = { editingItemIndex = -1 }
                            )
                        } else {
                            // Display mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.displayExpression,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1
                                    )
                                    if (item.productNameSnapshot != null) {
                                        Text(
                                            text = item.productNameSnapshot,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                Text(
                                    text = MoneyUtils.formatPrice(item.calculatedAmount),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                // Edit button (only for product items with known unit price)
                                if (item.unitPriceSnapshot != null) {
                                    IconButton(
                                        onClick = {
                                            editingItemIndex = index
                                            editingItemQtyText = item.quantityOrWeight
                                                ?.stripTrailingZeros()?.toPlainString() ?: "1"
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                // Delete button
                                IconButton(
                                    onClick = {
                                        if (editableItems.size > 1) {
                                            editableItems.removeAt(index)
                                            if (editingItemIndex >= editableItems.size) {
                                                editingItemIndex = -1
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = if (editableItems.size > 1)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Add Item button
                TextButton(
                    onClick = { showAddItemDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item", fontWeight = FontWeight.SemiBold)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // ── Totals ───────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Calculated Total:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = MoneyUtils.formatPrice(calculatedTotal),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Final price input
                OutlinedTextField(
                    value = finalPriceText,
                    onValueChange = { finalPriceText = it },
                    label = { Text("Final Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    prefix = { Text("₹ ") }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // ── Payment Method ───────────────────────────────────────────
                Text(
                    text = "Payment Method:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EditPaymentChip(
                        label = "💵 Cash",
                        isSelected = selectedPayment == PaymentMethod.CASH,
                        activeColor = CashGreen,
                        onClick = {
                            selectedPayment = if (selectedPayment == PaymentMethod.CASH) null
                            else PaymentMethod.CASH
                        },
                        modifier = Modifier.weight(1f)
                    )
                    EditPaymentChip(
                        label = "📱 UPI",
                        isSelected = selectedPayment == PaymentMethod.UPI,
                        activeColor = UpiBlue,
                        onClick = {
                            selectedPayment = if (selectedPayment == PaymentMethod.UPI) null
                            else PaymentMethod.UPI
                        },
                        modifier = Modifier.weight(1f)
                    )
                    EditPaymentChip(
                        label = "⏳ Pending",
                        isSelected = selectedPayment == PaymentMethod.PENDING,
                        activeColor = PendingOrange,
                        onClick = {
                            selectedPayment = if (selectedPayment == PaymentMethod.PENDING) null
                            else PaymentMethod.PENDING
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Customer info for Pending / Pay-Later
                if (selectedPayment == PaymentMethod.PENDING) {
                    OutlinedTextField(
                        value = customerNameInput,
                        onValueChange = { customerNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Customer Name / Phone (Optional)") },
                        placeholder = { Text("e.g. Ramesh, Stall #4") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (editableItems.isNotEmpty()) {
                        val parsedFinal = finalPriceText.trim().toBigDecimalOrNull()
                        // If final price equals calculated total, pass null (no override)
                        val finalAmt = if (parsedFinal != null &&
                            parsedFinal.setScale(2, RoundingMode.HALF_UP).compareTo(calculatedTotal) != 0
                        ) parsedFinal else null
                        onSave(bill.id, editableItems.toList(), finalAmt, selectedPayment, customerNameInput.trim().ifEmpty { null })
                    }
                },
                enabled = editableItems.isNotEmpty()
            ) {
                Text("💾 Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Add freeform item sub-dialog
    if (showAddItemDialog) {
        AddFreeformItemDialog(
            onAdd = { name, amount ->
                val itemName = name.trim().ifEmpty { "Custom Item" }
                val newItem = BillItem(
                    billId = bill.id,
                    expression = "$itemName: ₹${amount.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()}",
                    calculatedAmount = amount.setScale(2, RoundingMode.HALF_UP),
                    productNameSnapshot = itemName,
                    createdAt = System.currentTimeMillis()
                )
                editableItems.add(newItem)
                showAddItemDialog = false
            },
            onDismiss = { showAddItemDialog = false }
        )
    }
}

@Composable
private fun EditItemInline(
    item: BillItem,
    qtyText: String,
    onQtyChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = item.productNameSnapshot ?: "Item",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = qtyText,
                onValueChange = onQtyChange,
                label = { Text("Qty / Weight") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                suffix = {
                    if (item.unit != null) {
                        Text(item.unit.unitLabel.trim(), fontSize = 12.sp)
                    }
                }
            )
            Button(
                onClick = onConfirm,
                modifier = Modifier.height(40.dp)
            ) {
                Text("✓", fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onCancel,
                modifier = Modifier.height(40.dp)
            ) {
                Text("✗")
            }
        }
        // Preview the new amount
        val previewQty = qtyText.trim().toBigDecimalOrNull()
        if (previewQty != null && previewQty > BigDecimal.ZERO && item.unitPriceSnapshot != null) {
            val previewAmount = (item.unitPriceSnapshot * previewQty).setScale(2, RoundingMode.HALF_UP)
            Text(
                text = "= ${MoneyUtils.formatPrice(previewAmount)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun AddFreeformItemDialog(
    onAdd: (name: String, amount: BigDecimal) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Item", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    prefix = { Text("₹ ") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amountText.trim().toBigDecimalOrNull()
                    if (parsed != null && parsed > BigDecimal.ZERO) {
                        onAdd(name, parsed)
                    }
                },
                enabled = amountText.trim().toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true
            ) {
                Text("Add", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditPaymentChip(
    label: String,
    isSelected: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor.copy(alpha = 0.18f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) BorderStroke(1.5.dp, activeColor)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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
