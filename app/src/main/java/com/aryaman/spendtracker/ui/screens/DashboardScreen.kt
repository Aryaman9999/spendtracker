package com.aryaman.spendtracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryaman.spendtracker.data.local.dao.CategorySpendSummary
import com.aryaman.spendtracker.data.local.dao.RoommateBalance
import com.aryaman.spendtracker.data.local.entity.TransactionEntity
import com.aryaman.spendtracker.data.model.TransactionDirection
import com.aryaman.spendtracker.ui.PeriodType
import com.aryaman.spendtracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    transactions: List<TransactionEntity>,
    categorySpends: List<CategorySpendSummary>,
    roommateBalances: List<RoommateBalance>,
    pendingCount: Int,
    periodType: PeriodType,
    periodLabel: String,
    isCurrentPeriod: Boolean,
    canGoForward: Boolean,
    onPeriodTypeChange: (PeriodType) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onResetToCurrentPeriod: () -> Unit,
    onNavigateToTransactions: (filterPending: Boolean, categoryFilter: String?) -> Unit,
    onNavigateToSplits: () -> Unit
) {
    val totalSpend = categorySpends.sumOf { it.totalAmount }
    val totalOwedByRoommates = roommateBalances.sumOf { it.totalUnsettled }

    // Daily pace calculation
    val divisorDays = remember(periodType) {
        if (periodType == PeriodType.WEEK) 7
        else {
            val cal = Calendar.getInstance()
            cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        }
    }
    val dailyAverage = if (divisorDays > 0) totalSpend / divisorDays else 0.0

    // No giant TopAppBar - everything is seamlessly placed inside LazyColumn with natural padding
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Sleek, Low-Profile Brand Header (Replaces bulky TopAppBar)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(PrimaryIndigo, Color(0xFF6366F1))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "₹",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Row {
                        Text(
                            "Spend",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Tracker",
                            fontWeight = FontWeight.Normal,
                            fontSize = 20.sp,
                            color = PrimaryIndigoDark
                        )
                    }
                }

                if (pendingCount > 0) {
                    IconButton(
                        onClick = { onNavigateToTransactions(true, null) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = AmberPending,
                                    contentColor = Color.White
                                ) { Text(pendingCount.toString()) }
                            }
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Pending Review",
                                tint = AmberPending,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Time Period Selector: [ Week | Month ] Pills + ‹ Date Navigator ›
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Segmented Pills: Week / Month
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            val isWeek = periodType == PeriodType.WEEK
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isWeek) PrimaryIndigo else Color.Transparent)
                                    .clickable { onPeriodTypeChange(PeriodType.WEEK) }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Week",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isWeek) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isWeek) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (!isWeek) PrimaryIndigo else Color.Transparent)
                                    .clickable { onPeriodTypeChange(PeriodType.MONTH) }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Month",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (!isWeek) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isWeek) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Period Navigator: ‹ Label ›
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onPreviousPeriod,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Previous Period",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = periodLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clickable { if (!isCurrentPeriod) onResetToCurrentPeriod() }
                                .padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = onNextPeriod,
                            enabled = canGoForward,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Next Period",
                                tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Pending Review Banner (if any uncategorized spends)
        if (pendingCount > 0) {
            item {
                Surface(
                    color = AmberContainer.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTransactions(true, null) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$pendingCount Uncategorized Spend${if (pendingCount > 1) "s" else ""}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AmberPending
                            )
                            Text(
                                text = "Tap to categorize & teach rules",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { onNavigateToTransactions(true, null) },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPending),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Review", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 4. Hero Spend Card - Clean, Well-Padded Fintech Card
        item {
            HeroSpendCard(
                periodLabel = periodLabel,
                periodType = periodType,
                totalSpend = totalSpend,
                dailyAverage = dailyAverage,
                transactionCount = transactions.size
            )
        }

        // 5. Quick Roommate Balance Card (if owed money)
        if (totalOwedByRoommates > 0) {
            item {
                RoommateOwedQuickCard(
                    totalOwed = totalOwedByRoommates,
                    onNavigateToSplits = onNavigateToSplits
                )
            }
        }

        // 6. Graphical Spending Analysis by Category
        item {
            SpendingAnalyticsCard(
                categorySpends = categorySpends,
                totalSpend = totalSpend,
                periodLabel = periodLabel,
                onCategoryClick = { category ->
                    onNavigateToTransactions(false, category)
                }
            )
        }

        // 7. Recent Transactions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { onNavigateToTransactions(false, null) }) {
                    Text(
                        text = "See All (${transactions.size})",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigoDark,
                        fontSize = 13.sp
                    )
                }
            }
        }

        val recentList = transactions.take(5)
        if (recentList.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No transactions logged in this period",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Switch time periods or wait for incoming bank SMS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentList, key = { it.id }) { txn ->
                TransactionRowItem(transaction = txn)
            }
        }

        item { Spacer(Modifier.height(28.dp)) }
    }
}

/**
 * High-End Fintech Hero Balance Card.
 */
@Composable
fun HeroSpendCard(
    periodLabel: String,
    periodType: PeriodType,
    totalSpend: Double,
    dailyAverage: Double,
    transactionCount: Int
) {
    val spendWhole = totalSpend.toLong()
    val spendCents = String.format(Locale.getDefault(), "%02d", ((totalSpend - spendWhole) * 100).toInt().coerceAtLeast(0))
    val periodHeadline = if (periodType == PeriodType.WEEK) "TOTAL SPENT THIS WEEK" else "TOTAL SPENT THIS MONTH"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2E1065),
                        Color(0xFF1E1B4B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column {
            // Header Row: Period tag & Personal Share chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = periodLabel.uppercase(Locale.ROOT),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldSuccess.copy(alpha = 0.20f)
                ) {
                    Text(
                        text = "✓ Personal Share",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6EE7B7),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Main Currency Display
            Text(
                text = periodHeadline,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "₹",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(bottom = 6.dp, end = 2.dp)
                )
                Text(
                    text = String.format(Locale.getDefault(), "%,d", spendWhole),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = ".$spendCents",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Embedded Sub-Metrics Row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Daily average
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF93C5FD),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (periodType == PeriodType.WEEK) "Daily Avg" else "Daily Pace",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "₹%.0f/day", dailyAverage),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )

                    // Transaction count
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Activity",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                            Text(
                                text = "$transactionCount spends",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern Card for Roommates Unsettled Balance.
 */
@Composable
fun RoommateOwedQuickCard(
    totalOwed: Double,
    onNavigateToSplits: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToSplits),
        shape = RoundedCornerShape(16.dp),
        color = EmeraldContainer.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldSuccess.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Roommates Owe You",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldSuccess
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "₹%.2f", totalOwed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Button(
                onClick = onNavigateToSplits,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ledger", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                }
            }
        }
    }
}

/**
 * Graphical Spending Analysis Section.
 */
@Composable
fun SpendingAnalyticsCard(
    categorySpends: List<CategorySpendSummary>,
    totalSpend: Double,
    periodLabel: String,
    onCategoryClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spending Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$periodLabel • ${categorySpends.size} categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "₹%.2f", totalSpend),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (categorySpends.isEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "No categorized expenses in this period.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(Modifier.height(14.dp))

                // Multi-segment Proportional Spending Bar (Graphical Analysis Tool)
                SpendProportionalBar(
                    categorySpends = categorySpends,
                    totalSpend = totalSpend
                )

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Spacer(Modifier.height(6.dp))

                // Compact Category List Rows with Real Vector Icons
                categorySpends.forEachIndexed { index, cat ->
                    val percentage = if (totalSpend > 0) (cat.totalAmount / totalSpend).toFloat() else 0f
                    val catColor = getCategoryColor(cat.category)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onCategoryClick(cat.category) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            CategoryAvatar(category = cat.category, size = 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = cat.category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                LinearProgressIndicator(
                                    progress = { percentage },
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = catColor,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.getDefault(), "₹%.2f", cat.totalAmount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%.0f%%", percentage * 100),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = catColor
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (index < categorySpends.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                            modifier = Modifier.padding(start = 46.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * A sleek segmented proportional spending bar.
 */
@Composable
fun SpendProportionalBar(
    categorySpends: List<CategorySpendSummary>,
    totalSpend: Double
) {
    if (totalSpend <= 0) return

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            categorySpends.forEachIndexed { _, cat ->
                val ratio = (cat.totalAmount / totalSpend).toFloat()
                if (ratio > 0.01f) {
                    Box(
                        modifier = Modifier
                            .weight(ratio)
                            .fillMaxHeight()
                            .background(getCategoryColor(cat.category))
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Category Color Dots Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            categorySpends.take(4).forEach { cat ->
                val color = getCategoryColor(cat.category)
                val pct = ((cat.totalAmount / totalSpend) * 100).toInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${cat.category.split(" ").first()} $pct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Modern compact Transaction Row Item for Dashboard recent transactions.
 */
@Composable
fun TransactionRowItem(transaction: TransactionEntity) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val isDebit = transaction.direction == TransactionDirection.DEBIT
    val isSplit = transaction.myActualShare != transaction.amount

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MerchantAvatar(
                merchantName = transaction.cleanMerchant ?: "Unknown Payee",
                isDebit = isDebit,
                category = transaction.category,
                size = 40.dp
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.cleanMerchant ?: "Unknown Payee",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${transaction.category ?: "Uncategorized"} • ${dateFormat.format(Date(transaction.timestampMillis))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (isSplit) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PrimaryIndigo.copy(alpha = 0.15f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = "✂️ Split",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryIndigoDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = (if (isDebit) "- " else "+ ") + String.format(Locale.getDefault(), "₹%.2f", transaction.myActualShare),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDebit) RoseDebit else EmeraldSuccess
                )
                if (isSplit) {
                    Text(
                        text = "Bill ₹${String.format(Locale.getDefault(), "%.0f", transaction.amount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

