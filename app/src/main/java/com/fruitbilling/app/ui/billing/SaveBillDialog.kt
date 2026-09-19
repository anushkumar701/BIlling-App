package com.fruitbilling.app.ui.billing

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitbilling.app.data.model.BillWithItems
import com.fruitbilling.app.data.model.PaymentMethod
import com.fruitbilling.app.ui.theme.CashGreen
import com.fruitbilling.app.ui.theme.UpiBlue
import com.fruitbilling.app.util.MoneyUtils
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * SaveBillDialog — Human Cashier Review Workflow:
 *
 * When the cashier taps "Save Bill", this dialog pops up showing:
 *  1. Bill number & item count
 *  2. Itemized list of all products & calculations in this bill
 *  3. Calculated Total
 *  4. Optional Final Price override (e.g. round off / discount)
 *  5. Payment mode selector (Cash / UPI)
 *  6. "Cancel / Add More" and "Confirm & Save Bill" buttons
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
    onDismiss: () -> Unit
) {
    val bill = billWithItems.bill
    val items = billWithItems.items
    val calculatedTotal = bill.calculatedTotal

    var finalPriceText by remember { mutableStateOf(initialFinalPrice) }
    var selectedPayment by remember { mutableStateOf(initialPaymentMethod) }

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
                // 1. Scrollable itemized product list
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
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

                                Text(
                                    text = MoneyUtils.formatPrice(item.calculatedAmount),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 4.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )
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

                // Quick Rounding helpers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val roundDownTen = calculatedTotal.divide(BigDecimal("10"), 0, RoundingMode.FLOOR).multiply(BigDecimal("10"))
                    if (roundDownTen < calculatedTotal && roundDownTen > BigDecimal.ZERO) {
                        Surface(
                            onClick = { finalPriceText = roundDownTen.stripTrailingZeros().toPlainString() },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Round ₹${roundDownTen.toPlainString()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = { finalPriceText = "" },
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Exact Total",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }

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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmSave(finalPriceText, selectedPayment)
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "💾 Save Bill (${MoneyUtils.formatPrice(effectiveAmount)})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
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
