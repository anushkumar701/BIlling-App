package com.fruitbilling.app.ui.billing

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.fruitbilling.app.data.model.BillItem
import com.fruitbilling.app.util.MoneyUtils

@Composable
fun DeleteCalculationDialog(
    item: BillItem,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete this calculation?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "${item.displayExpression} (${MoneyUtils.formatPrice(item.calculatedAmount)})"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
