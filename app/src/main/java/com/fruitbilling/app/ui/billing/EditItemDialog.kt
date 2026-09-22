package com.fruitbilling.app.ui.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fruitbilling.app.data.model.BillItem
import com.fruitbilling.app.data.model.ProductUnit
import com.fruitbilling.app.util.MoneyUtils
import java.math.BigDecimal

@Composable
fun EditItemDialog(
    item: BillItem,
    inputText: String,
    calculatedAmount: BigDecimal,
    isValid: Boolean,
    errorMessage: String?,
    onInputChanged: (String) -> Unit,
    onConfirmEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val isProduct = !item.productNameSnapshot.isNullOrBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isProduct) {
                        "Edit ${item.productNameSnapshot}"
                    } else {
                        "Edit Calculation"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (isProduct && item.unitPriceSnapshot != null) {
                    Text(
                        text = "Rate: ${MoneyUtils.formatPrice(item.unitPriceSnapshot)}${item.unit?.unitLabel ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
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
                // Quick chips for products
                if (isProduct) {
                    val presets = if (item.unit == ProductUnit.KG) {
                        listOf("0.25" to "250g", "0.5" to "500g", "1" to "1 kg", "2" to "2 kg")
                    } else {
                        listOf("1" to "1 pc", "2" to "2 pcs", "5" to "5 pcs", "10" to "10 pcs")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (value, label) ->
                            val isSelected = inputText.trim() == value
                            Surface(
                                onClick = { onInputChanged(value) },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { input ->
                        if (isProduct) {
                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                onInputChanged(input)
                            }
                        } else {
                            onInputChanged(input)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = if (isProduct) {
                                "Quantity / Weight (${item.unit?.displayName ?: "Qty"})"
                            } else {
                                "Calculation / Expression"
                            }
                        )
                    },
                    suffix = if (isProduct && item.unit != null) {
                        { Text(item.unit.unitLabel.trim()) }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isProduct) KeyboardType.Decimal else KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (isValid) onConfirmEdit() }),
                    isError = errorMessage != null,
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Amount:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = MoneyUtils.formatPrice(calculatedAmount),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmEdit,
                enabled = isValid,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Update", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
