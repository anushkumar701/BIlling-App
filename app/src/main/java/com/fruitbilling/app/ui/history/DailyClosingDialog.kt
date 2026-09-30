package com.fruitbilling.app.ui.history

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fruitbilling.app.data.model.BillWithItems
import com.fruitbilling.app.data.model.PaymentMethod
import com.fruitbilling.app.data.preferences.ShopPreferences
import com.fruitbilling.app.util.DateUtils
import com.fruitbilling.app.util.MoneyUtils
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DailyClosingDialog(
    todayBills: List<BillWithItems>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var countedCashText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    // Financial calculations
    val todayCash = todayBills
        .filter { it.bill.paymentMethod == PaymentMethod.CASH }
        .map { it.bill.effectiveChargedAmount }
        .fold(BigDecimal.ZERO, BigDecimal::add)

    val todayUpi = todayBills
        .filter { it.bill.paymentMethod == PaymentMethod.UPI }
        .map { it.bill.effectiveChargedAmount }
        .fold(BigDecimal.ZERO, BigDecimal::add)

    val todayPending = todayBills
        .filter { it.bill.paymentMethod == PaymentMethod.PENDING || it.bill.paymentMethod == null }
        .map { it.bill.effectiveChargedAmount }
        .fold(BigDecimal.ZERO, BigDecimal::add)

    val totalCollected = todayCash.add(todayUpi)
    val totalRevenue = totalCollected.add(todayPending)

    val countedCash = countedCashText.trim().toBigDecimalOrNull()
    val cashDifference = countedCash?.subtract(todayCash)

    val shopName = remember { ShopPreferences.getShopName(context) }
    val todayDateStr = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🌙 Daily Closing (Z-Report)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "$shopName • $todayDateStr",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Breakdown Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ClosingRow(
                            label = "💵 Cash Expected in Drawer:",
                            value = MoneyUtils.formatPrice(todayCash),
                            isBold = true,
                            valueColor = Color(0xFF2E7D32)
                        )
                        ClosingRow(
                            label = "📱 UPI Collections:",
                            value = MoneyUtils.formatPrice(todayUpi),
                            isBold = false,
                            valueColor = Color(0xFF1565C0)
                        )
                        ClosingRow(
                            label = "⏳ Pending (Uncollected):",
                            value = MoneyUtils.formatPrice(todayPending),
                            isBold = false,
                            valueColor = Color(0xFFEF6C00)
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        ClosingRow(
                            label = "Total Realized Sales:",
                            value = MoneyUtils.formatPrice(totalCollected),
                            isBold = true,
                            valueColor = MaterialTheme.colorScheme.onSurface
                        )
                        if (todayPending > BigDecimal.ZERO) {
                            ClosingRow(
                                label = "Total Gross Sales (with Pending):",
                                value = MoneyUtils.formatPrice(totalRevenue),
                                isBold = true,
                                valueColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        ClosingRow(
                            label = "Total Bills Closed:",
                            value = "${todayBills.size} bills",
                            isBold = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cash Reconciliation Input
                Text(
                    text = "Cash Drawer Reconciliation",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = countedCashText,
                    onValueChange = { countedCashText = it },
                    label = { Text("Actual Cash Counted in Drawer (${MoneyUtils.currencySymbol})") },
                    placeholder = { Text("e.g. ${todayCash.toInt()}") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Discrepancy Display
                if (cashDifference != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val isBalanced = cashDifference.compareTo(BigDecimal.ZERO) == 0
                    val isShortage = cashDifference < BigDecimal.ZERO

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isBalanced -> Color(0xFFE8F5E9)
                                isShortage -> Color(0xFFFFEBEE)
                                else -> Color(0xFFE3F2FD)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isBalanced) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = when {
                                    isBalanced -> Color(0xFF2E7D32)
                                    isShortage -> Color(0xFFC62828)
                                    else -> Color(0xFF1565C0)
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = when {
                                        isBalanced -> "✅ Perfectly Balanced (${MoneyUtils.currencySymbol}0.00)"
                                        isShortage -> "⚠️ Cash Shortage: ${MoneyUtils.formatPrice(cashDifference.abs())}"
                                        else -> "ℹ️ Cash Surplus: +${MoneyUtils.formatPrice(cashDifference)}"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isBalanced -> Color(0xFF2E7D32)
                                            isShortage -> Color(0xFFC62828)
                                            else -> Color(0xFF1565C0)
                                        }
                                    )
                                )
                                Text(
                                    text = when {
                                        isBalanced -> "Physical cash matches recorded cash sales."
                                        isShortage -> "Drawer has less cash than recorded sales."
                                        else -> "Drawer has more cash than recorded sales."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Closing Notes
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Closing Notes (Optional)") },
                    placeholder = { Text("e.g. Paid ${MoneyUtils.currencySymbol}200 for tea/supplies from drawer...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            shareClosingReport(
                                context = context,
                                shopName = shopName,
                                dateStr = todayDateStr,
                                billsCount = todayBills.size,
                                cash = todayCash,
                                upi = todayUpi,
                                pending = todayPending,
                                totalCollected = totalCollected,
                                countedCash = countedCash,
                                difference = cashDifference,
                                notes = notesText.trim()
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Report", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClosingRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
                color = valueColor
            )
        )
    }
}

private fun shareClosingReport(
    context: Context,
    shopName: String,
    dateStr: String,
    billsCount: Int,
    cash: BigDecimal,
    upi: BigDecimal,
    pending: BigDecimal,
    totalCollected: BigDecimal,
    countedCash: BigDecimal?,
    difference: BigDecimal?,
    notes: String
) {
    val report = buildString {
        appendLine("🏪 *$shopName — Daily Closing Report (Z-Report)*")
        appendLine("📅 Date: $dateStr")
        appendLine("──────────────────────────────")
        appendLine("🧾 Total Bills: $billsCount")
        appendLine("💵 Cash Sales: ${MoneyUtils.formatPrice(cash)}")
        appendLine("📱 UPI Collections: ${MoneyUtils.formatPrice(upi)}")
        if (pending > BigDecimal.ZERO) {
            appendLine("⏳ Pending Dues: ${MoneyUtils.formatPrice(pending)}")
        }
        appendLine("──────────────────────────────")
        appendLine("💰 *TOTAL REALIZED: ${MoneyUtils.formatPrice(totalCollected)}*")
        appendLine("──────────────────────────────")

        if (countedCash != null) {
            appendLine("🔍 *Cash Drawer Reconciliation:*")
            appendLine("• Expected Cash: ${MoneyUtils.formatPrice(cash)}")
            appendLine("• Counted Cash:  ${MoneyUtils.formatPrice(countedCash)}")
            if (difference != null) {
                val diffStr = when {
                    difference.compareTo(BigDecimal.ZERO) == 0 -> "✅ Balanced (${MoneyUtils.currencySymbol}0.00)"
                    difference < BigDecimal.ZERO -> "⚠️ Shortage: -${MoneyUtils.formatPrice(difference.abs())}"
                    else -> "ℹ️ Surplus: +${MoneyUtils.formatPrice(difference)}"
                }
                appendLine("• Difference:   $diffStr")
            }
            appendLine("──────────────────────────────")
        }

        if (notes.isNotBlank()) {
            appendLine("📝 *Cashier Notes:*")
            appendLine(notes)
            appendLine("──────────────────────────────")
        }

        appendLine("Generated via Retail Billing POS")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "$shopName Daily Closing - $dateStr")
        putExtra(Intent.EXTRA_TEXT, report)
    }
    context.startActivity(Intent.createChooser(intent, "Share Daily Closing Report"))
}
