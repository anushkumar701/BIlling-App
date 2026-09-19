package com.fruitbilling.app.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitbilling.app.data.model.BillWithItems
import com.fruitbilling.app.data.model.PaymentMethod
import com.fruitbilling.app.data.repository.DaySalesSummary
import com.fruitbilling.app.data.repository.MonthSalesSummary
import com.fruitbilling.app.data.repository.TodayStats
import com.fruitbilling.app.ui.theme.CashGreen
import com.fruitbilling.app.ui.theme.UpiBlue
import com.fruitbilling.app.util.DateUtils
import com.fruitbilling.app.util.MoneyUtils

import android.widget.Toast
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import com.fruitbilling.app.util.CsvExportManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "History & Summary",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    actions = {
                        if (uiState.completedBills.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    val res = CsvExportManager.exportBillsToCsv(context, uiState.completedBills)
                                    res.onSuccess { file ->
                                        CsvExportManager.shareCsvFile(context, file)
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Export failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export CSV",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                TabRow(
                    selectedTabIndex = uiState.selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    HistoryTab.entries.forEach { tab ->
                        Tab(
                            selected = uiState.selectedTab == tab,
                            onClick = { viewModel.onTabSelected(tab) },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        when (uiState.selectedTab) {
            HistoryTab.BILLS -> {
                BillsListContent(
                    bills = uiState.filteredBills,
                    totalCompletedCount = uiState.completedBills.size,
                    pendingCount = uiState.pendingCount,
                    cashCount = uiState.cashCount,
                    upiCount = uiState.upiCount,
                    todayStats = uiState.todayStats,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = viewModel::onFilterSelected,
                    onUpdatePaymentMethod = viewModel::onUpdatePaymentMethod,
                    onBillSelected = viewModel::onBillSelected,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            HistoryTab.SUMMARY -> {
                BusinessSummaryContent(
                    summary = uiState.businessSummary,
                    completedBills = uiState.completedBills,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        // Bill Detail Dialog (with retrospective Cash/UPI assignment and WhatsApp share)
        uiState.selectedBillForDetail?.let { selectedBill ->
            BillDetailDialog(
                billWithItems = selectedBill,
                onUpdatePaymentMethod = { method ->
                    viewModel.onUpdatePaymentMethod(selectedBill.bill.id, method)
                },
                onDismiss = viewModel::onDismissDetail
            )
        }
    }
}

@Composable
private fun BillsListContent(
    bills: List<BillWithItems>,
    totalCompletedCount: Int,
    pendingCount: Int,
    cashCount: Int,
    upiCount: Int,
    todayStats: TodayStats,
    selectedFilter: PaymentFilter,
    onFilterSelected: (PaymentFilter) -> Unit,
    onUpdatePaymentMethod: (Long, PaymentMethod?) -> Unit,
    onBillSelected: (BillWithItems) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── 1. Today's Live Sales Dashboard ──────────────────────────────────
        item {
            TodayOverviewCard(
                todayStats = todayStats,
                onFilterPending = { onFilterSelected(PaymentFilter.PENDING) }
            )
        }

        // ── 2. Payment Filter Chips ──────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilterChip(
                    label = "All ($totalCompletedCount)",
                    isSelected = selectedFilter == PaymentFilter.ALL,
                    onClick = { onFilterSelected(PaymentFilter.ALL) }
                )
                HistoryFilterChip(
                    label = "⚠️ Pending ($pendingCount)",
                    isSelected = selectedFilter == PaymentFilter.PENDING,
                    onClick = { onFilterSelected(PaymentFilter.PENDING) },
                    isWarning = pendingCount > 0
                )
                HistoryFilterChip(
                    label = "💵 Cash ($cashCount)",
                    isSelected = selectedFilter == PaymentFilter.CASH,
                    onClick = { onFilterSelected(PaymentFilter.CASH) }
                )
                HistoryFilterChip(
                    label = "📱 UPI ($upiCount)",
                    isSelected = selectedFilter == PaymentFilter.UPI,
                    onClick = { onFilterSelected(PaymentFilter.UPI) }
                )
            }
        }

        // ── 3. Bills List / Empty State ──────────────────────────────────────
        if (bills.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (totalCompletedCount == 0)
                            "No completed bills yet.\nCompleted bills from the Billing tab will appear here."
                        else
                            "No bills match the selected filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(bills, key = { it.bill.id }) { billWithItems ->
                HistoryBillRow(
                    billWithItems = billWithItems,
                    onClick = { onBillSelected(billWithItems) },
                    onSetPayment = { method ->
                        onUpdatePaymentMethod(billWithItems.bill.id, method)
                    }
                )
            }
        }
    }
}

@Composable
private fun TodayOverviewCard(
    todayStats: TodayStats,
    onFilterPending: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Sales",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${todayStats.todayCompletedBillCount} ${if (todayStats.todayCompletedBillCount == 1) "bill" else "bills"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = MoneyUtils.formatPrice(todayStats.todaySales),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 26.sp
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Cash", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = MoneyUtils.formatWholePrice(todayStats.cashSales),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = CashGreen)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("UPI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = MoneyUtils.formatWholePrice(todayStats.upiSales),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = UpiBlue)
                        )
                    }
                }
            }

            // Unspecified warning banner if any bills today lack payment method
            if (todayStats.unspecifiedBillCount > 0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onFilterPending() }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${todayStats.unspecifiedBillCount} bill(s) pending payment mode (${MoneyUtils.formatWholePrice(todayStats.unspecifiedSales)}) — Tap to review",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isWarning: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isWarning -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.height(34.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isWarning -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            )
        }
    }
}

@Composable
fun HistoryBillRow(
    billWithItems: BillWithItems,
    onClick: () -> Unit,
    onSetPayment: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    val bill = billWithItems.bill
    val isUpi = bill.paymentMethod == PaymentMethod.UPI
    val isCash = bill.paymentMethod == PaymentMethod.CASH
    val isUnspecified = bill.paymentMethod == null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bill number and date
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bill.formattedBillNumber,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    bill.completedAt?.let { timestamp ->
                        Text(
                            text = DateUtils.formatBillTimestamp(timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Calculations count
                Text(
                    text = "${billWithItems.items.size} ${if (billWithItems.items.size == 1) "calc" else "calcs"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // Final charged amount
                Text(
                    text = MoneyUtils.formatPrice(bill.effectiveChargedAmount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.padding(start = 6.dp, end = 8.dp)
                )

                // Payment badge (if already set)
                if (!isUnspecified) {
                    Surface(
                        color = when {
                            isUpi -> UpiBlue.copy(alpha = 0.12f)
                            isCash -> CashGreen.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = bill.paymentMethod?.label ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isUpi) UpiBlue else CashGreen
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // If payment is unspecified, offer 1-tap quick Cash / UPI buttons directly on row!
            // No "Unspecified" button exists: if cashier taps neither, it stays unspecified.
            if (isUnspecified) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Set Pay:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.outline
                    )

                    Surface(
                        onClick = { onSetPayment(PaymentMethod.CASH) },
                        shape = RoundedCornerShape(6.dp),
                        color = CashGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, CashGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "💵 Cash",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CashGreen
                                )
                            )
                        }
                    }

                    Surface(
                        onClick = { onSetPayment(PaymentMethod.UPI) },
                        shape = RoundedCornerShape(6.dp),
                        color = UpiBlue.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, UpiBlue.copy(alpha = 0.5f)),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "📱 UPI / GPay",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = UpiBlue
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Business Summary Screen (§16):
 * For the shopkeeper to check at home:
 * A simple month total block (Total Sales, Total Bills, Cash, UPI, Average Bill)
 * followed by every date's sales and bill count, newest first, shown directly
 * with NO date picker required to see it.
 */
@Composable
private fun BusinessSummaryContent(
    summary: MonthSalesSummary,
    completedBills: List<BillWithItems> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Month Header Block
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Month Total (${summary.monthString.ifEmpty { "Current Month" }})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Sales:",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = MoneyUtils.formatPrice(summary.totalSales),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryPill(label = "Bills", value = "${summary.totalBills}")
                        SummaryPill(label = "Cash", value = MoneyUtils.formatWholePrice(summary.cashSales))
                        SummaryPill(label = "UPI", value = MoneyUtils.formatWholePrice(summary.upiSales))
                        SummaryPill(label = "Avg Bill", value = MoneyUtils.formatWholePrice(summary.avgBill))
                    }
                }
            }
        }

        // Export Sales to CSV / Excel Card
        if (completedBills.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val res = CsvExportManager.exportBillsToCsv(context, completedBills)
                            res.onSuccess { file ->
                                CsvExportManager.shareCsvFile(context, file)
                            }.onFailure { err ->
                                Toast.makeText(context, "Export failed: ${err.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "📊", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Export Sales to Excel / CSV",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Clean report with all bills, item formulas & payment totals",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Daily Breakdown Header
        item {
            Text(
                text = "Daily Sales Breakdown (Newest First)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        // Every Date's Sales & Bill Count (no date picker required!)
        if (summary.dailyBreakdown.isEmpty()) {
            item {
                Text(
                    text = "No recorded sales yet.",
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(summary.dailyBreakdown, key = { it.dateString }) { day ->
                DailySummaryCard(day = day)
            }
        }
    }
}

@Composable
private fun SummaryPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun DailySummaryCard(day: DaySalesSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = day.dateString,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "${day.billCount} ${if (day.billCount == 1) "bill" else "bills"} (Cash: ${MoneyUtils.formatWholePrice(day.cashSales)}, UPI: ${MoneyUtils.formatWholePrice(day.upiSales)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Text(
                text = MoneyUtils.formatPrice(day.totalSales),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
