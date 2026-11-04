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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryaman.spendtracker.data.local.dao.CategorySpendSummary
import com.aryaman.spendtracker.data.local.dao.RoommateBalance
import com.aryaman.spendtracker.data.local.entity.CategoryEntity
import com.aryaman.spendtracker.data.local.entity.TransactionEntity
import com.aryaman.spendtracker.data.model.TransactionDirection
import com.aryaman.spendtracker.ui.PeriodType
import com.aryaman.spendtracker.ui.components.SetCategoryBudgetDialog
import com.aryaman.spendtracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class BarChartItem(
    val label: String,
    val amount: Double,
    val isHighlighted: Boolean
)

@Composable
fun DashboardScreen(
    transactions: List<TransactionEntity>,
    categorySpends: List<CategorySpendSummary>,
    roommateBalances: List<RoommateBalance>,
    categories: List<CategoryEntity> = emptyList(),
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
    onNavigateToSplits: () -> Unit,
    onUpdateCategoryBudget: (categoryName: String, budget: Double?) -> Unit = { _, _ -> }
) {
    val totalSpend = categorySpends.sumOf { it.totalAmount }
    val totalOwedByRoommates = roommateBalances.sumOf { it.totalUnsettled }

    // Prepare chart bars based on periodType
    val (chartBars, dailyAverage, highestSpendItem) = remember(transactions, periodType, totalSpend, isCurrentPeriod) {
        val debitTxns = transactions.filter { it.direction == TransactionDirection.DEBIT }

        when (periodType) {
            PeriodType.DAY -> {
                // 5 time blocks for the day (Morning, Midday, Afternoon, Evening, Night)
                val blocks = listOf(
                    "6AM" to (6..9),
                    "10AM" to (10..13),
                    "2PM" to (14..17),
                    "6PM" to (18..21),
                    "10PM" to (22..24)
                )
                val calBlock = Calendar.getInstance()
                val bars = blocks.map { (label, hourRange) ->
                    val sum = debitTxns.filter {
                        calBlock.timeInMillis = it.timestampMillis
                        calBlock.get(Calendar.HOUR_OF_DAY) in hourRange
                    }.sumOf { it.myActualShare }
                    BarChartItem(label, sum, isHighlighted = false)
                }
                val highest = debitTxns.maxByOrNull { it.myActualShare }?.let {
                    Triple(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it.timestampMillis)), it.myActualShare, it.category ?: "Spend")
                }
                Triple(bars, totalSpend, highest)
            }
            PeriodType.WEEK -> {
                // 7 days of the week: Mon to Sun
                val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val dayConstants = listOf(
                    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                    Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
                )
                val realTodayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                val calDay = Calendar.getInstance()
                val bars = dayNames.mapIndexed { idx, name ->
                    val dow = dayConstants[idx]
                    val sum = debitTxns.filter {
                        calDay.timeInMillis = it.timestampMillis
                        calDay.get(Calendar.DAY_OF_WEEK) == dow
                    }.sumOf { it.myActualShare }
                    BarChartItem(
                        label = name,
                        amount = sum,
                        isHighlighted = (isCurrentPeriod && dow == realTodayDow)
                    )
                }
                val avg = totalSpend / 7.0
                val highest = bars.maxByOrNull { it.amount }?.let {
                    if (it.amount > 0) Triple(it.label, it.amount, "Weekly Peak") else null
                }
                Triple(bars, avg, highest)
            }
            PeriodType.MONTH -> {
                val realToday = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                val cal = Calendar.getInstance()
                val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val calDay = Calendar.getInstance()
                val daySums = (1..totalDaysInMonth).map { day ->
                    val sum = debitTxns.filter {
                        calDay.timeInMillis = it.timestampMillis
                        calDay.get(Calendar.DAY_OF_MONTH) == day
                    }.sumOf { it.myActualShare }
                    BarChartItem(
                        label = if (day == 1 || day == 5 || day == 10 || day == 15 || day == 20 || day == 25 || (isCurrentPeriod && day == realToday)) {
                            if (isCurrentPeriod && day == realToday) "Today" else day.toString()
                        } else "",
                        amount = sum,
                        isHighlighted = (isCurrentPeriod && day == realToday)
                    )
                }
                val daysElapsed = if (isCurrentPeriod) realToday.coerceAtLeast(1) else totalDaysInMonth
                val avg = totalSpend / daysElapsed
                val highestDayIndex = daySums.indices.maxByOrNull { daySums[it].amount } ?: -1
                val highest = if (highestDayIndex >= 0 && daySums[highestDayIndex].amount > 0) {
                    val dayNum = highestDayIndex + 1
                    val topCatForDay = debitTxns.filter {
                        calDay.timeInMillis = it.timestampMillis
                        calDay.get(Calendar.DAY_OF_MONTH) == dayNum
                    }.groupBy { it.category }.maxByOrNull { entry -> entry.value.sumOf { it.myActualShare } }?.key ?: "Spend"
                    Triple("$dayNum ${SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)}", daySums[highestDayIndex].amount, topCatForDay)
                } else null
                Triple(daySums, avg, highest)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Sleek Brand Header (Low-Profile)
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
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(listOf(PrimaryIndigo, Color(0xFF6366F1)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("₹", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Row {
                        Text("Spend", fontWeight = FontWeight.Black, fontSize = 19.sp, color = MaterialTheme.colorScheme.onBackground)
                        Text("Tracker", fontWeight = FontWeight.Normal, fontSize = 19.sp, color = PrimaryIndigoDark)
                    }
                }

                if (pendingCount > 0) {
                    IconButton(
                        onClick = { onNavigateToTransactions(true, null) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = AmberPending, contentColor = Color.White) {
                                    Text(pendingCount.toString())
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Pending Review", tint = AmberPending, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // 2. Headline Metric & Time Period Tabs (Direct Step Tracker Design)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Large Spend Metric
                Text(
                    text = String.format(Locale.getDefault(), "₹%.2f", totalSpend),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "$periodLabel • ${transactions.size} spends logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(14.dp))

                // [ Day | Week | Month ] Pill Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        PeriodType.DAY to "Day",
                        PeriodType.WEEK to "Week",
                        PeriodType.MONTH to "Month"
                    ).forEach { (type, label) ->
                        val isSelected = periodType == type
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onPeriodTypeChange(type) }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 9.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                var dragDistanceX by remember { mutableFloatStateOf(0f) }
                val timeSliderModifier = Modifier.pointerInput(periodType, periodLabel, canGoForward) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragDistanceX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            dragDistanceX += dragAmount
                            if (kotlin.math.abs(dragAmount) > 2f) {
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            val threshold = 35.dp.toPx()
                            if (dragDistanceX > threshold) {
                                // Swiped right -> go to previous period
                                onPreviousPeriod()
                            } else if (dragDistanceX < -threshold) {
                                // Swiped left -> go to next period
                                if (canGoForward) {
                                    onNextPeriod()
                                }
                            }
                            dragDistanceX = 0f
                        },
                        onDragCancel = {
                            dragDistanceX = 0f
                        }
                    )
                }

                // Date Navigator Row: ‹ Date Label › with swipe support
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(timeSliderModifier),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousPeriod,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clickable { if (!isCurrentPeriod) onResetToCurrentPeriod() }
                            .padding(horizontal = 8.dp)
                    )
                    IconButton(
                        onClick = onNextPeriod,
                        enabled = canGoForward,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            tint = if (canGoForward) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 3. Step Tracker Inspired Vertical Bar Chart Card (Supports horizontal swipe left/right to shift periods)
        item {
            var chartDragX by remember { mutableFloatStateOf(0f) }
            val chartSwipeModifier = Modifier.pointerInput(periodType, periodLabel, canGoForward) {
                detectHorizontalDragGestures(
                    onDragStart = { chartDragX = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        chartDragX += dragAmount
                        if (kotlin.math.abs(dragAmount) > 2f) {
                            change.consume()
                        }
                    },
                    onDragEnd = {
                        val threshold = 35.dp.toPx()
                        if (chartDragX > threshold) {
                            onPreviousPeriod()
                        } else if (chartDragX < -threshold) {
                            if (canGoForward) {
                                onNextPeriod()
                            }
                        }
                        chartDragX = 0f
                    },
                    onDragCancel = {
                        chartDragX = 0f
                    }
                )
            }

            StepTrackerSpendChartCard(
                chartBars = chartBars,
                totalSpend = totalSpend,
                dailyAverage = dailyAverage,
                periodType = periodType,
                isCurrentPeriod = isCurrentPeriod,
                modifier = chartSwipeModifier
            )
        }

        // 4. "Highest Single-Day Spend" Highlight Card (Inspired by Step Tracker Record Card)
        if (highestSpendItem != null && highestSpendItem.second > 0) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Highest single-day spend",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = highestSpendItem.first,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "₹%.2f", highestSpendItem.second),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = RoseDebitDark
                            )
                            Text(
                                text = highestSpendItem.third,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // 5. Roommates Unsettled Balance Widget
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
                categories = categories,
                transactions = transactions,
                onUpdateCategoryBudget = onUpdateCategoryBudget,
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
 * Step Tracker Style Vertical Bar Chart Card.
 * Displays vertical pill bars for each time unit with top summary statistics.
 */
@Composable
fun StepTrackerSpendChartCard(
    chartBars: List<BarChartItem>,
    totalSpend: Double,
    dailyAverage: Double,
    periodType: PeriodType,
    isCurrentPeriod: Boolean = true,
    modifier: Modifier = Modifier
) {
    val maxBarAmount = remember(chartBars) {
        val maxAmount = chartBars.maxOfOrNull { it.amount } ?: 0.0
        if (maxAmount > 0.0) maxAmount else 100.0
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            // Chart Canvas Area: Vertical Rounded Bars
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                // Bars Row
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    chartBars.forEach { item ->
                        val hasSpend = item.amount > 0.0
                        val ratio = if (hasSpend && maxBarAmount > 0.0) {
                            (item.amount / maxBarAmount).toFloat().coerceIn(0.08f, 1f)
                        } else {
                            0f
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            // The Bar: Background pill track + Foreground filled pill
                            Box(
                                modifier = Modifier
                                    .width(if (periodType == PeriodType.MONTH) 6.dp else 22.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                if (hasSpend && ratio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(ratio)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                if (item.isHighlighted && isCurrentPeriod) EmeraldSuccess
                                                else EmeraldSuccess.copy(alpha = 0.85f)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                // X-Axis Labels Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    chartBars.forEach { item ->
                        if (item.label.isNotEmpty()) {
                            Text(
                                text = item.label,
                                fontSize = 9.sp,
                                fontWeight = if (item.isHighlighted && isCurrentPeriod) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (item.isHighlighted && isCurrentPeriod) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            Spacer(Modifier.height(12.dp))

            // Two-column Summary Stats (Matching Step Tracker layout: Step count & Distance)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Spend",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "₹%.2f", totalSpend),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (periodType == PeriodType.WEEK) "Daily Average" else "Daily Pace",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "₹%.0f/day", dailyAverage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
 * Graphical Spending Analysis Section with Category Budget Limits & Exemption Support.
 */
@Composable
fun SpendingAnalyticsCard(
    categorySpends: List<CategorySpendSummary>,
    totalSpend: Double,
    periodLabel: String,
    categories: List<CategoryEntity> = emptyList(),
    transactions: List<TransactionEntity> = emptyList(),
    onUpdateCategoryBudget: (categoryName: String, budget: Double?) -> Unit = { _, _ -> },
    onCategoryClick: (String) -> Unit
) {
    var categoryToSetBudget by remember { mutableStateOf<Pair<String, Double?>?>(null) }

    categoryToSetBudget?.let { (catName, currentBudget) ->
        SetCategoryBudgetDialog(
            categoryName = catName,
            currentBudget = currentBudget,
            onDismiss = { categoryToSetBudget = null },
            onSaveBudget = { newBudget ->
                onUpdateCategoryBudget(catName, newBudget)
                categoryToSetBudget = null
            }
        )
    }

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
                        text = "Spending by Category",
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

                // Compact Category List Rows with Real Vector Icons & Budget Progress
                categorySpends.forEachIndexed { index, cat ->
                    val percentage = if (totalSpend > 0) (cat.totalAmount / totalSpend).toFloat() else 0f
                    val catColor = getCategoryColor(cat.category)

                    // Find configured budget for this category
                    val catEntity = categories.find { it.name.equals(cat.category, ignoreCase = true) }
                    val monthlyBudget = catEntity?.monthlyBudget

                    // Calculate spend strictly counted against budget (excluding transactions with isBudgetExempt)
                    val budgetedAmount = remember(transactions, cat.category) {
                        transactions.filter {
                            it.direction == TransactionDirection.DEBIT &&
                            it.category.equals(cat.category, ignoreCase = true) &&
                            !it.isBudgetExempt
                        }.sumOf { it.myActualShare }
                    }
                    val exemptAmount = cat.totalAmount - budgetedAmount

                    val hasBudget = monthlyBudget != null && monthlyBudget > 0
                    val budgetRatio = if (hasBudget) (budgetedAmount / monthlyBudget!!).toFloat() else 0f
                    val isOverBudget = hasBudget && budgetedAmount > monthlyBudget!!

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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = cat.category,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { categoryToSetBudget = cat.category to monthlyBudget },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AccountBalanceWallet,
                                            contentDescription = "Set Budget",
                                            tint = if (hasBudget) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { if (hasBudget) budgetRatio.coerceIn(0f, 1f) else percentage },
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (isOverBudget) RoseDebitDark else catColor,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                if (hasBudget) {
                                    Text(
                                        text = "₹${String.format(Locale.getDefault(), "%.0f", budgetedAmount)} / ₹${String.format(Locale.getDefault(), "%.0f", monthlyBudget)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOverBudget) RoseDebitDark else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isOverBudget) {
                                        Text(
                                            text = "Over by ₹${String.format(Locale.getDefault(), "%.0f", budgetedAmount - monthlyBudget)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = RoseDebitDark
                                        )
                                    } else {
                                        Text(
                                            text = "${(budgetRatio * 100).toInt()}% • ₹${String.format(Locale.getDefault(), "%.0f", monthlyBudget - budgetedAmount)} left",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = catColor
                                        )
                                    }
                                } else {
                                    Text(
                                        text = String.format(Locale.getDefault(), "₹%.2f", cat.totalAmount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.0f%% of total", percentage * 100),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = catColor
                                    )
                                }
                                if (exemptAmount > 0) {
                                    Text(
                                        text = "(₹${String.format(Locale.getDefault(), "%.0f", exemptAmount)} off-budget)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    )
                                }
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
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
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
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${transaction.category ?: "Uncategorized"} • ${dateFormat.format(Date(transaction.timestampMillis))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                    color = if (isDebit) RoseDebitDark else EmeraldSuccess
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
