package com.fruitbilling.app.ui.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitbilling.app.data.model.BillStatus
import com.fruitbilling.app.data.model.BillWithItems
import com.fruitbilling.app.data.model.BillingMode
import com.fruitbilling.app.data.repository.TodayStats
import com.fruitbilling.app.util.MoneyUtils

@Composable
fun BillingHeader(
    currentBill: BillWithItems?,
    activeAndHeldBills: List<BillWithItems>,
    billingMode: BillingMode,
    todayStats: TodayStats,
    onHoldBill: () -> Unit,
    onSwitchBill: (Long) -> Unit,
    onNewBill: () -> Unit,
    onToggleMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Deliberate tap-based bill selector (#024 ▾)
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            .clickable { isDropdownExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentBill?.bill?.formattedBillNumber ?: "#001",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "▾",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        Text(
                            text = "Active & Held Bills",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        activeAndHeldBills.forEach { billWithItems ->
                            val bill = billWithItems.bill
                            val isSelected = bill.id == currentBill?.bill?.id
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${bill.formattedBillNumber} (${if (bill.status == BillStatus.HELD) "HELD" else "ACTIVE"})",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = MoneyUtils.formatPrice(bill.calculatedTotal),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                },
                                onClick = {
                                    isDropdownExpanded = false
                                    onSwitchBill(bill.id)
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "+ New Draft Bill",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            onClick = {
                                isDropdownExpanded = false
                                onNewBill()
                            }
                        )
                    }
                }

                // Middle: Mode pill (Experienced / Beginner) & Today's quick sales info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Today Sales pill — abbreviated to prevent truncation on small screens
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        val salesStr = run {
                            val v = todayStats.todaySales
                            when {
                                v >= java.math.BigDecimal(1_00_000) -> "₹${(v.toDouble() / 1_00_000).let { if (it >= 10) it.toInt().toString() else String.format("%.1f", it) }}L"
                                v >= java.math.BigDecimal(1_000) -> "₹${(v.toDouble() / 1_000).let { if (it >= 10) it.toInt().toString() else String.format("%.1f", it) }}K"
                                else -> "₹${v.setScale(0, java.math.RoundingMode.HALF_UP)}"
                            }
                        }
                        Text(
                            text = "$salesStr · ${todayStats.todayCompletedBillCount}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Mode switch toggle
                    Surface(
                        color = if (billingMode == BillingMode.EXPERIENCED)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { onToggleMode() }
                    ) {
                        Text(
                            text = if (billingMode == BillingMode.EXPERIENCED) "⚡ Fast" else "🔍 Search",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Right: Hold button (§3)
                OutlinedButton(
                    onClick = onHoldBill,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Hold",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
