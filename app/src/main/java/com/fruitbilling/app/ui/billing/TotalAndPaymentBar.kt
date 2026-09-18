package com.fruitbilling.app.ui.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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

                // Final Price chip / trigger -- sized as a comfortable touch target,
                // not just clickable text, per the app's accessibility requirement.
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

        // Payment Method Selector Chips (Cash / UPI / Unspecified) (§9)
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
        }
    }

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
                    // Final price is allowed to exceed the calculated total (the owner may
                    // have a deliberate reason), but flag it so a typo doesn't slip through
                    // silently -- this never blocks "Set", just warns (§9).
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
