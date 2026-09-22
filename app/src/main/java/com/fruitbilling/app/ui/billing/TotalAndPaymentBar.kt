package com.fruitbilling.app.ui.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    finalPriceInput: String = "",
    selectedPaymentMethod: PaymentMethod?,
    onFinalPriceChanged: (String) -> Unit = {},
    onPaymentMethodSelected: (PaymentMethod?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showChangeDialog by remember { mutableStateOf(false) }

    val effectiveTotal = if (finalPriceInput.isNotBlank()) {
        finalPriceInput.toBigDecimalOrNull() ?: calculatedTotal
    } else {
        calculatedTotal
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Total Display (Left)
        Column {
            Text(
                text = "TOTAL",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            )
            Text(
                text = MoneyUtils.formatPrice(effectiveTotal),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 24.sp
                )
            )
        }

        // Payment Chips (Right)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PaymentChip(
                label = "💵 Cash",
                isSelected = selectedPaymentMethod == PaymentMethod.CASH,
                activeColor = CashGreen,
                onClick = {
                    if (selectedPaymentMethod == PaymentMethod.CASH) {
                        onPaymentMethodSelected(null)
                    } else {
                        onPaymentMethodSelected(PaymentMethod.CASH)
                    }
                }
            )

            PaymentChip(
                label = "📱 UPI",
                isSelected = selectedPaymentMethod == PaymentMethod.UPI,
                activeColor = UpiBlue,
                onClick = {
                    if (selectedPaymentMethod == PaymentMethod.UPI) {
                        onPaymentMethodSelected(null)
                    } else {
                        onPaymentMethodSelected(PaymentMethod.UPI)
                    }
                }
            )

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
                        modifier = Modifier.padding(horizontal = 8.dp)
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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

                // Quick Tendered Notes (Dynamic and strictly >= total)
                Text(
                    text = "Quick Cash Given:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.outline
                )

                val suggestions = MoneyUtils.getCashTenderSuggestions(totalAmount)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.forEach { (label, value) ->
                        val isSelected = cashReceivedInput == value
                        Surface(
                            onClick = { cashReceivedInput = value },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) CashGreen.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, if (isSelected) CashGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) CashGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = cashReceivedInput,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                            cashReceivedInput = input
                        }
                    },
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
