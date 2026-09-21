package com.fruitbilling.app.ui.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitbilling.app.data.model.BillItem
import com.fruitbilling.app.data.model.BillWithItems
import com.fruitbilling.app.data.model.PaymentMethod
import com.fruitbilling.app.ui.theme.CashGreen
import com.fruitbilling.app.ui.theme.UpiBlue
import com.fruitbilling.app.util.MoneyUtils
import com.fruitbilling.app.util.ReceiptUtils
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * SaveBillDialog — Human Cashier Review Workflow:
 *
 * When the cashier taps "Save Bill", this dialog pops up showing:
 *  1. Bill number & item count
 *  2. Itemized list of all products & calculations with 1-tap delete
 *  3. Calculated Total
 *  4. Optional Final Price override (e.g. round off / discount)
 *  5. Payment mode selector (Cash / UPI)
 *  6. Cash Tender & Return Change calculation (for Cash sales)
 *  7. "Share Receipt" via WhatsApp / text
 *  8. "Cancel / Add More" and "Confirm & Save Bill" buttons
 *
 * The bill is ONLY saved when the cashier confirms here.
 */
@Composable
fun SaveBillDialog(
    billWithItems: BillWithItems,
    initialFinalPrice: String,
    initialPaymentMethod: PaymentMethod?,
    isSaving: Boolean,
    onConfirmSave: (finalPriceText: String, paymentMethod: PaymentMethod?) -> Unit,
    onDismiss: () -> Unit,
    onDeleteItem: ((BillItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val bill = billWithItems.bill
    val items = billWithItems.items
    val calculatedTotal = bill.calculatedTotal

    var finalPriceText by remember { mutableStateOf(initialFinalPrice) }
    var selectedPayment by remember { mutableStateOf(initialPaymentMethod) }
    var cashTenderedInput by remember { mutableStateOf("") }

    val parsedFinal = finalPriceText.trim().toBigDecimalOrNull()
    val effectiveAmount = when {
        parsedFinal != null && parsedFinal >= BigDecimal.ZERO -> parsedFinal
        else -> calculatedTotal
    }

    val discount = if (parsedFinal != null && parsedFinal < calculatedTotal) {
        calculatedTotal.subtract(parsedFinal)
    } else null

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = bill.formattedBillNumber,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Review & Save",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "${items.size} ${if (items.size == 1) "item" else "items"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Scrollable itemized product list with 1-tap delete
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    if (items.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No items in bill",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(items, key = { it.id }) { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val title = item.productNameSnapshot ?: item.displayExpression
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.productNameSnapshot != null) {
                                            Text(
                                                text = item.displayExpression,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = MoneyUtils.formatPrice(item.calculatedAmount),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                        if (onDeleteItem != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                onClick = { onDeleteItem(item) },
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove item",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                )
                            }
                        }
                    }
                }

                // 2. Calculated total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calculated Total:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = MoneyUtils.formatPrice(calculatedTotal),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                // 3. Final Price (Optional Discount / Override)
                OutlinedTextField(
                    value = finalPriceText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                            finalPriceText = input
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Final Price (Optional Discount)") },
                    placeholder = { Text(MoneyUtils.formatPrice(calculatedTotal)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        if (discount != null && discount > BigDecimal.ZERO) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "-${MoneyUtils.formatPrice(discount)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                )

                // 4. Payment Mode Selection
                Text(
                    text = "Payment Mode:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.outline
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cash Option
                    val isCashSelected = selectedPayment == PaymentMethod.CASH
                    Surface(
                        onClick = {
                            selectedPayment = if (isCashSelected) null else PaymentMethod.CASH
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCashSelected) CashGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isCashSelected) 2.dp else 1.dp,
                            color = if (isCashSelected) CashGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💵 Cash",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isCashSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCashSelected) CashGreen else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            if (isCashSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CashGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // UPI Option
                    val isUpiSelected = selectedPayment == PaymentMethod.UPI
                    Surface(
                        onClick = {
                            selectedPayment = if (isUpiSelected) null else PaymentMethod.UPI
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isUpiSelected) UpiBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isUpiSelected) 2.dp else 1.dp,
                            color = if (isUpiSelected) UpiBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📱 UPI / GPay",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isUpiSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isUpiSelected) UpiBlue else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            if (isUpiSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = UpiBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // 5. Cash Tender & Return Change (Human Cashier Helper)
                if (selectedPayment == PaymentMethod.CASH) {
                    Surface(
                        color = CashGreen.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CashGreen.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "💵 Cash Tender & Change",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CashGreen
                                )
                            )

                            // Quick Tender Note Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val exactVal = effectiveAmount.setScale(0, RoundingMode.CEILING).toPlainString()
                                val chips = listOf(
                                    "Exact" to exactVal,
                                    "₹100" to "100",
                                    "₹200" to "200",
                                    "₹500" to "500"
                                )
                                chips.forEach { (label, value) ->
                                    val isSelected = cashTenderedInput == value
                                    Surface(
                                        onClick = { cashTenderedInput = value },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) CashGreen.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (isSelected) CashGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                        modifier = Modifier.weight(1f).height(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }

                            // Custom cash tendered input
                            OutlinedTextField(
                                value = cashTenderedInput,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                        cashTenderedInput = input
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Cash Received (₹)") },
                                placeholder = { Text("e.g. 500") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Return change display badge
                            val tendered = cashTenderedInput.toBigDecimalOrNull()
                            if (tendered != null) {
                                val change = tendered.subtract(effectiveAmount)
                                val isEnough = change >= BigDecimal.ZERO
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isEnough) CashGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (isEnough) CashGreen else MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isEnough) "RETURN CHANGE:" else "SHORT / STILL DUE:",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isEnough) CashGreen else MaterialTheme.colorScheme.error
                                            )
                                        )
                                        Text(
                                            text = MoneyUtils.formatPrice(change.abs()),
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isEnough) CashGreen else MaterialTheme.colorScheme.error
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share Receipt button
                OutlinedButton(
                    onClick = {
                        val tendered = cashTenderedInput.toBigDecimalOrNull()
                        val change = if (tendered != null && tendered >= effectiveAmount) tendered.subtract(effectiveAmount) else null
                        val receiptText = ReceiptUtils.generateReceiptText(billWithItems, changeAmount = change)
                        ReceiptUtils.shareReceipt(context, receiptText, "Share Bill ${bill.formattedBillNumber}")
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        onConfirmSave(finalPriceText, selectedPayment)
                    },
                    enabled = !isSaving && items.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "💾 Save Bill (${MoneyUtils.formatPrice(effectiveAmount)})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel / Add More")
            }
        }
    )
}
