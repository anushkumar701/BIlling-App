package com.fruitbilling.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitbilling.app.data.model.BillWithItems
import com.fruitbilling.app.data.model.PaymentMethod
import com.fruitbilling.app.data.repository.DaySalesSummary
import com.fruitbilling.app.data.repository.MonthSalesSummary
import com.fruitbilling.app.ui.theme.CashGreen
import com.fruitbilling.app.ui.theme.UpiBlue
import com.fruitbilling.app.util.DateUtils
import com.fruitbilling.app.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    bills = uiState.completedBills,
                    onBillSelected = viewModel::onBillSelected,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            HistoryTab.SUMMARY -> {
                BusinessSummaryContent(
                    summary = uiState.businessSummary,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        // Read-only Detail Dialog
        uiState.selectedBillForDetail?.let { selectedBill ->
            BillDetailDialog(
                billWithItems = selectedBill,
                onDismiss = viewModel::onDismissDetail
            )
        }
    }
}

@Composable
private fun BillsListContent(
    bills: List<BillWithItems>,
    onBillSelected: (BillWithItems) -> Unit,
    modifier: Modifier = Modifier
) {
    if (bills.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No completed bills yet.\nCompleted bills from the Billing tab will appear here.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(bills, key = { it.bill.id }) { billWithItems ->
                HistoryBillRow(
                    billWithItems = billWithItems,
                    onClick = { onBillSelected(billWithItems) }
                )
            }
        }
    }
}

@Composable
fun HistoryBillRow(
    billWithItems: BillWithItems,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bill = billWithItems.bill
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Final amount
            Text(
                text = MoneyUtils.formatPrice(bill.effectiveChargedAmount),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Payment badge
            val isUpi = bill.paymentMethod == PaymentMethod.UPI
            val isCash = bill.paymentMethod == PaymentMethod.CASH
            Surface(
                color = when {
                    isUpi -> UpiBlue.copy(alpha = 0.12f)
                    isCash -> CashGreen.copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = bill.paymentMethod?.label ?: "Unspecified",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isUpi -> UpiBlue
                            isCash -> CashGreen
                            else -> MaterialTheme.colorScheme.outline
                        }
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
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
    modifier: Modifier = Modifier
) {
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
