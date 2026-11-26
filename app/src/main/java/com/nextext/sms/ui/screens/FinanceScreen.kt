package com.nextext.sms.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nextext.sms.data.local.TransactionEntity
import com.nextext.sms.domain.model.TransactionType
import com.nextext.sms.ui.components.TopAmbientGlow
import com.nextext.sms.ui.components.TopNavDestinationTabs
import com.nextext.sms.ui.util.Formatters
import com.nextext.sms.ui.viewmodel.AccountCardSummary
import com.nextext.sms.ui.viewmodel.FinanceDashboardState

private enum class TxFilter(val label: String) {
    ALL("All"),
    DEBITS("Debits"),
    CREDITS("Credits"),
    UPI("UPI"),
    CARDS("Cards")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    state: FinanceDashboardState,
    useMaterial3Design: Boolean = false,
    swapMenuBarPositions: Boolean = false,
    unreadMessagesCount: Int = 0,
    onNavigateToMessages: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onOpenThread: (Long, String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(TxFilter.ALL) }

    val filteredTx = remember(state.transactions, selectedFilter) {
        when (selectedFilter) {
            TxFilter.ALL -> state.transactions
            TxFilter.DEBITS -> state.transactions.filter { it.type.isOutflow }
            TxFilter.CREDITS -> state.transactions.filter { !it.type.isOutflow }
            TxFilter.UPI -> state.transactions.filter { it.type == TransactionType.UPI }
            TxFilter.CARDS -> state.transactions.filter { it.type == TransactionType.CARD_SPEND }
        }
    }

    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (!useMaterial3Design) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        if (!useMaterial3Design) {
            TopAmbientGlow(
                accentColor = accentColor,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = if (!useMaterial3Design) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        title = { Text("Passbook & Expenses", fontWeight = FontWeight.Bold) }
                    )
                    if (!useMaterial3Design && swapMenuBarPositions) {
                        TopNavDestinationTabs(
                            selectedTab = 2,
                            unreadMessagesCount = unreadMessagesCount,
                            accentColor = accentColor,
                            onSelectTab = { tabIndex ->
                                when (tabIndex) {
                                    0 -> onNavigateToMessages()
                                    1 -> onNavigateToReminders()
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
        if (state.transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("No bank or UPI transactions yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "NexText automatically tracks debits, credits, UPI payments, and card balances on-device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = if (!useMaterial3Design) 112.dp else 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    MonthlyOverviewCard(state = state)
                }

                if (state.accounts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Accounts & Cards",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.accounts, key = { it.key }) { account ->
                                AccountSummaryCard(account)
                            }
                        }
                    }
                }

                if (state.spendByCategory.isNotEmpty()) {
                    item {
                        SpendBreakdownCard(state = state)
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TxFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label) }
                            )
                        }
                    }
                }

                items(filteredTx, key = { it.id }) { tx ->
                    TransactionRowCard(
                        tx = tx,
                        onClick = { onOpenThread(tx.threadId, tx.address) }
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun MonthlyOverviewCard(state: FinanceDashboardState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "This Month Overview",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = Formatters.formatCurrency(state.monthlyDebitTotal, state.primaryCurrency),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB91C1C)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Credited",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = Formatters.formatCurrency(state.monthlyCreditTotal, state.primaryCurrency),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSummaryCard(account: AccountCardSummary) {
    Card(
        modifier = Modifier.width(196.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = account.institution,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = account.accountLast4?.let { "A/c ••••$it" } ?: "General Account",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            if (account.latestBalance != null) {
                Text(
                    text = "Avl: ${Formatters.formatCurrency(account.latestBalance, account.currency)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "${account.transactionCount} transactions",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SpendBreakdownCard(state: FinanceDashboardState) {
    val total = state.monthlyDebitTotal.coerceAtLeast(1.0)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Spend by Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            state.spendByCategory.take(5).forEach { (category, amount) ->
                val progress = (amount / total).toFloat().coerceIn(0f, 1f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(category.label, style = MaterialTheme.typography.bodySmall)
                    Text(
                        Formatters.formatCurrency(amount, state.primaryCurrency),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TransactionRowCard(
    tx: TransactionEntity,
    onClick: () -> Unit
) {
    val isOutflow = tx.type.isOutflow
    val amountColor = if (isOutflow) Color(0xFFDC2626) else Color(0xFF15803D)
    val prefix = if (isOutflow) "-" else "+"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.merchant ?: tx.institution ?: tx.address,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(
                        tx.type.label,
                        tx.spendCategory.label,
                        tx.accountLast4?.let { "••$it" }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = Formatters.formatDateTime(tx.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix${Formatters.formatCurrency(tx.amount, tx.currency)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                tx.balanceAfter?.let { bal ->
                    Text(
                        text = "Bal ${Formatters.formatCurrency(bal, tx.currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
