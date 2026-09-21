package com.fruitbilling.app.ui.menu

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitbilling.app.data.model.Product
import com.fruitbilling.app.data.model.ProductUnit
import com.fruitbilling.app.util.ProductImageUtils
import com.fruitbilling.app.util.rememberProductImage

@Composable
fun AddEditProductDialog(
    product: Product?,
    onSave: (id: Long?, name: String, price: String, unit: ProductUnit, iconRef: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEdit = product != null
    var name by remember { mutableStateOf(product?.name ?: "") }
    var priceText by remember {
        mutableStateOf(
            product?.price?.stripTrailingZeros()?.toPlainString() ?: ""
        )
    }
    var unit by remember { mutableStateOf(product?.unit ?: ProductUnit.KG) }
    var iconRef by remember { mutableStateOf(product?.iconRef) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = ProductImageUtils.saveImage(context, it)
            if (savedPath != null) {
                // If user selected an image previously in this session that is not the saved product's image, clean it up
                if (iconRef != null && iconRef != product?.iconRef && iconRef != savedPath) {
                    ProductImageUtils.deleteImage(iconRef)
                }
                iconRef = savedPath
            }
        }
    }

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
                    placeholder = { Text("e.g. Papaya, Orange") },
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

                // Optional Product Image Section
                Text(
                    text = "Product Image (Optional):",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                val imageBitmap = rememberProductImage(iconRef)
                if (imageBitmap != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Product preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Change Image", fontSize = 12.sp)
                        }

                        TextButton(
                            onClick = {
                                if (iconRef != product?.iconRef) {
                                    ProductImageUtils.deleteImage(iconRef)
                                }
                                iconRef = null
                            }
                        ) {
                            Text("Remove", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select Image (Optional)", fontSize = 13.sp)
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
                    onSave(product?.id, name, priceText, unit, iconRef)
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
