package com.fruitbilling.app.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fruitbilling.app.data.model.Product
import com.fruitbilling.app.data.model.ProductUnit

@Composable
fun AddEditProductDialog(
    product: Product?,
    onSave: (id: Long?, name: String, price: String, unit: ProductUnit) -> Unit,
    onDismiss: () -> Unit
) {
    val isEdit = product != null
    var name by remember { mutableStateOf(product?.name ?: "") }
    var priceText by remember {
        mutableStateOf(
            product?.price?.stripTrailingZeros()?.toPlainString() ?: ""
        )
    }
    var unit by remember { mutableStateOf(product?.unit ?: ProductUnit.KG) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Edit Product" else "Add New Product",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Product Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Product Name") },
                    placeholder = { Text("e.g. Papali, Orange") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                // Price
                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        errorText = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Price (₹)") },
                    placeholder = { Text("e.g. 70") },
                    prefix = { Text("₹", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // Unit selection
                Text(
                    text = "Unit:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // KG option
                    Surface(
                        onClick = { unit = ProductUnit.KG },
                        shape = RoundedCornerShape(8.dp),
                        color = if (unit == ProductUnit.KG) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            if (unit == ProductUnit.KG) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = unit == ProductUnit.KG,
                                onClick = { unit = ProductUnit.KG }
                            )
                            Text(text = "Per KG (/kg)")
                        }
                    }

                    // Piece option
                    Surface(
                        onClick = { unit = ProductUnit.PIECE },
                        shape = RoundedCornerShape(8.dp),
                        color = if (unit == ProductUnit.PIECE) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            if (unit == ProductUnit.PIECE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = unit == ProductUnit.PIECE,
                                onClick = { unit = ProductUnit.PIECE }
                            )
                            Text(text = "Per Piece (/pc)")
                        }
                    }
                }

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        errorText = "Product name cannot be empty."
                        return@Button
                    }
                    val p = priceText.trim().toBigDecimalOrNull()
                    if (p == null || p <= java.math.BigDecimal.ZERO) {
                        errorText = "Price must be greater than 0."
                        return@Button
                    }
                    onSave(product?.id, name, priceText, unit)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isEdit) "Update" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
