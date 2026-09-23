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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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

    // Smart cursor positioning and editing for weight units (e.g. "500 × 700g")
    var textFieldValue by remember(inputText) {
        val initialSelection = if (!isProduct) {
            when {
                inputText.endsWith("kg", ignoreCase = true) -> TextRange(inputText.length - 2)
                inputText.endsWith("g", ignoreCase = true) -> TextRange(inputText.length - 1)
                else -> TextRange(inputText.length)
            }
        } else {
            TextRange(inputText.length)
        }
        mutableStateOf(TextFieldValue(text = inputText, selection = initialSelection))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Edit ${if (isProduct) item.productNameSnapshot else "Item"}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
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
                    value = textFieldValue,
                    onValueChange = { newTfv ->
                        if (isProduct) {
                            val input = newTfv.text
                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                textFieldValue = newTfv
                                onInputChanged(input)
                            }
                        } else {
                            val oldText = textFieldValue.text
                            val newText = newTfv.text

                            // Smart weight backspace: when user presses erase on "...700g", erase the digit before 'g'
                            if (oldText.endsWith("g", ignoreCase = true) && !oldText.endsWith("kg", ignoreCase = true)) {
                                if (newText == oldText.dropLast(1)) {
                                    // User erased 'g' at the end -> erase the digit before 'g' instead and preserve 'g'
                                    val prefix = oldText.dropLast(1)
                                    val newPrefix = prefix.dropLast(1)
                                    val transformed = newPrefix + "g"
                                    val newPos = newPrefix.length.coerceAtLeast(0)
                                    val adjusted = TextFieldValue(text = transformed, selection = TextRange(newPos))
                                    textFieldValue = adjusted
                                    onInputChanged(transformed)
                                    return@OutlinedTextField
                                }
                                // If user typed a digit after 'g' (e.g. "...70g5"), move digit before 'g' -> "...705g"
                                val match = Regex("""^(.*)g(\d+)$""", RegexOption.IGNORE_CASE).find(newText)
                                if (match != null) {
                                    val before = match.groupValues[1]
                                    val digits = match.groupValues[2]
                                    val transformed = "$before$digits" + "g"
                                    val newPos = (before.length + digits.length).coerceAtLeast(0)
                                    val adjusted = TextFieldValue(text = transformed, selection = TextRange(newPos))
                                    textFieldValue = adjusted
                                    onInputChanged(transformed)
                                    return@OutlinedTextField
                                }
                            } else if (oldText.endsWith("kg", ignoreCase = true)) {
                                if (newText == oldText.dropLast(1) || newText == oldText.dropLast(2)) {
                                    val prefix = oldText.dropLast(2)
                                    val newPrefix = prefix.dropLast(1)
                                    val transformed = newPrefix + "kg"
                                    val newPos = newPrefix.length.coerceAtLeast(0)
                                    val adjusted = TextFieldValue(text = transformed, selection = TextRange(newPos))
                                    textFieldValue = adjusted
                                    onInputChanged(transformed)
                                    return@OutlinedTextField
                                }
                                val match = Regex("""^(.*)kg(\d+)$""", RegexOption.IGNORE_CASE).find(newText)
                                if (match != null) {
                                    val before = match.groupValues[1]
                                    val digits = match.groupValues[2]
                                    val transformed = "$before$digits" + "kg"
                                    val newPos = (before.length + digits.length).coerceAtLeast(0)
                                    val adjusted = TextFieldValue(text = transformed, selection = TextRange(newPos))
                                    textFieldValue = adjusted
                                    onInputChanged(transformed)
                                    return@OutlinedTextField
                                }
                            }

                            textFieldValue = newTfv
                            onInputChanged(newText)
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
