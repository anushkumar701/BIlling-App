package com.fruitbilling.app.ui.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitbilling.app.data.model.PaymentMethod
import com.fruitbilling.app.ui.theme.CashGreen
import com.fruitbilling.app.ui.theme.UpiBlue
import com.fruitbilling.app.util.MoneyUtils
import java.math.BigDecimal

@Composable
fun TotalAndPaymentBar(
    calculatedTotal: BigDecimal,
    finalPriceInput: String,
    selectedPaymentMethod: PaymentMethod?,
    onFinalPriceChanged: (String) -> Unit,
    onPaymentMethodSelected: (PaymentMethod?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFinalPriceDialog by remember { mutableStateOf(false) }
    var showChangeDialog by remember { mutableStateOf(false) }
    var tempFinalPrice by remember(finalPriceInput) { mutableStateOf(finalPriceInput) }

    val effectiveTotal = if (finalPriceInput.isNotBlank()) {
        finalPriceInput.toBigDecimalOrNull() ?: calculatedTotal
    } else {
        calculatedTotal
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Main Total Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TOTAL",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (finalPriceInput.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(Orig: ${MoneyUtils.formatPrice(calculatedTotal)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Final Price chip / trigger
                Text(
                    text = if (finalPriceInput.isNotBlank()) "Final ₹$finalPriceInput (tap to edit)" else "+ Set Final Price",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .clickable {
                            tempFinalPrice = finalPriceInput
                            showFinalPriceDialog = true
                        }
                        .padding(vertical = 6.dp, horizontal = 2.dp)
                )
            }

            Text(
                text = MoneyUtils.formatPrice(effectiveTotal),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 28.sp
                )
            )
        }

        // Payment Method Selector Chips (Cash / UPI / Unspecified) + Optional Change Calc
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pay:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.outline
            )

            // Cash chip
            PaymentChip(
                label = "Cash",
                isSelected = selectedPaymentMethod == PaymentMethod.CASH,
                activeColor = CashGreen,
                onClick = {
                    if (selectedPaymentMethod == PaymentMethod.CASH) {
                        onPaymentMethodSelected(null) // deselect to unspecified
                    } else {
                        onPaymentMethodSelected(PaymentMethod.CASH)
                    }
                },
                modifier = Modifier.weight(1f)
            )

            // UPI chip
            PaymentChip(
                label = "UPI",
                isSelected = selectedPaymentMethod == PaymentMethod.UPI,
                activeColor = UpiBlue,
                onClick = {
                    if (selectedPaymentMethod == PaymentMethod.UPI) {
                        onPaymentMethodSelected(null) // deselect to unspecified
                    } else {
                        onPaymentMethodSelected(PaymentMethod.UPI)
                    }
                },
                modifier = Modifier.weight(1f)
            )

            // Optional Cash Change Calculator trigger (visible when Cash is active or on-demand)
            if (selectedPaymentMethod == PaymentMethod.CASH) {
                Surface(
                    onClick = { showChangeDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    color = CashGreen.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, CashGreen.copy(alpha = 0.6f)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Text(
                            text = "🪙 Change",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CashGreen
                            )
                        )
                    }
                }
            }
        }
    }

    // Set Final Price Dialog
    if (showFinalPriceDialog) {
        AlertDialog(
            onDismissRequest = { showFinalPriceDialog = false },
            title = { Text("Set Final Price (Optional)", fontWeight = FontWeight.Bold) },
            text = {
                val parsedTemp = tempFinalPrice.trim().toBigDecimalOrNull()
                val exceedsTotal = parsedTemp != null && parsedTemp > calculatedTotal
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Calculated: ${MoneyUtils.formatPrice(calculatedTotal)}")
                    OutlinedTextField(
                        value = tempFinalPrice,
                        onValueChange = { tempFinalPrice = it },
                        placeholder = { Text("e.g. 350") },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (exceedsTotal) {
                        Text(
                            text = "This is higher than the calculated total — double-check it's not a typo.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onFinalPriceChanged(tempFinalPrice.trim())
                        showFinalPriceDialog = false
                    }
                ) {
                    Text("Set")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onFinalPriceChanged("")
                        showFinalPriceDialog = false
                    }
                ) {
                    Text("Clear / Cancel")
                }
            }
        )
    }

    // Optional Cash Change Calculator Dialog (§Tendered Cash)
    if (showChangeDialog) {
        CashChangeDialog(
            totalAmount = effectiveTotal,
            onDismiss = { showChangeDialog = false }
        )
    }
}

/**
 * Optional Tendered Cash Change Calculator:
 * Helps cashiers calculate return change quickly without mental math.
 */
@Composable
private fun CashChangeDialog(
    totalAmount: BigDecimal,
    onDismiss: () -> Unit
) {
    var cashReceivedInput by remember { mutableStateOf("") }
    val receivedAmount = cashReceivedInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val changeAmount = receivedAmount.subtract(totalAmount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🪙 Cash Change Calculator",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bill Total:", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = MoneyUtils.formatPrice(totalAmount),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Quick Tendered Notes
                Text(
                    text = "Quick Cash Given:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.outline
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("100", "200", "500").forEach { note ->
                        Surface(
                            onClick = { cashReceivedInput = note },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "₹$note",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = { cashReceivedInput = "1000" },
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "₹1000",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Surface(
                        onClick = { cashReceivedInput = totalAmount.setScale(0, java.math.RoundingMode.CEILING).toPlainString() },
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Exact",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = cashReceivedInput,
                    onValueChange = { cashReceivedInput = it },
                    placeholder = { Text("Enter Cash Given by Customer") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Large Return Change Display
                if (cashReceivedInput.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (changeAmount >= BigDecimal.ZERO)
                            CashGreen.copy(alpha = 0.12f)
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        border = BorderStroke(
                            1.dp,
                            if (changeAmount >= BigDecimal.ZERO) CashGreen else MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (changeAmount >= BigDecimal.ZERO) "RETURN CHANGE" else "SHORT / STILL DUE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (changeAmount >= BigDecimal.ZERO) CashGreen else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = MoneyUtils.formatPrice(changeAmount.abs()),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp,
                                    color = if (changeAmount >= BigDecimal.ZERO) CashGreen else MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CashGreen)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun PaymentChip(
    label: String,
    isSelected: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) BorderStroke(1.5.dp, activeColor) else null,
        modifier = modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
