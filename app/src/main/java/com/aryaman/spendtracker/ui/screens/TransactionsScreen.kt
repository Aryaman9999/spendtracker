package com.aryaman.spendtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryaman.spendtracker.data.local.entity.CategoryEntity
import com.aryaman.spendtracker.data.local.entity.SplitEntryEntity
import com.aryaman.spendtracker.data.local.entity.TransactionEntity
import com.aryaman.spendtracker.data.model.TransactionDirection
import com.aryaman.spendtracker.data.model.TransactionStatus
import com.aryaman.spendtracker.ui.components.CategoryPickerDialog
import com.aryaman.spendtracker.ui.components.SplitExpenseDialog
import com.aryaman.spendtracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TransactionFilter {
    ALL,
    PENDING,
    AUTO_MATCHED,
    SPLIT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    splits: List<SplitEntryEntity> = emptyList(),
    initialFilterPending: Boolean = false,
    selectedCategoryFilter: String? = null,
    onClearCategoryFilter: () -> Unit = {},
    onCategorize: (id: Long, category: String, createRule: Boolean) -> Unit,
    onSplit: (id: Long, myActualShare: Double, splits: List<SplitEntryEntity>) -> Unit,
    onDelete: (id: Long) -> Unit
) {
    var selectedFilter by remember {
        mutableStateOf(if (initialFilterPending) TransactionFilter.PENDING else TransactionFilter.ALL)
    }

    var transactionToCategorize by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToSplit by remember { mutableStateOf<TransactionEntity?>(null) }

    // Group splits by transactionId for instant O(1) lookup
    val splitsByTxnId = remember(splits) { splits.groupBy { it.transactionId } }

    val filteredList = remember(transactions, selectedFilter, selectedCategoryFilter) {
        val base = when (selectedFilter) {
            TransactionFilter.ALL -> transactions
            TransactionFilter.PENDING -> transactions.filter { it.status == TransactionStatus.PENDING_REVIEW }
            TransactionFilter.AUTO_MATCHED -> transactions.filter { it.status == TransactionStatus.AUTO_MATCHED }
            TransactionFilter.SPLIT -> transactions.filter { it.myActualShare != it.amount }
        }
        if (selectedCategoryFilter != null) {
            base.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
        } else {
            base
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Transactions", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Active Category Filter Banner
            if (selectedCategoryFilter != null) {
                val catColor = getCategoryColor(selectedCategoryFilter)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = MaterialTheme.shapes.small,
                    color = catColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryAvatar(category = selectedCategoryFilter, size = 32.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Filtered by Category",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = selectedCategoryFilter,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = onClearCategoryFilter,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == TransactionFilter.ALL,
                        onClick = { selectedFilter = TransactionFilter.ALL },
                        label = { Text("All (${transactions.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
                item {
                    val pendingCount = transactions.count { it.status == TransactionStatus.PENDING_REVIEW }
                    FilterChip(
                        selected = selectedFilter == TransactionFilter.PENDING,
                        onClick = { selectedFilter = TransactionFilter.PENDING },
                        label = { Text("Pending ($pendingCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == TransactionFilter.AUTO_MATCHED,
                        onClick = { selectedFilter = TransactionFilter.AUTO_MATCHED },
                        label = { Text("Auto-Matched") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
                item {
                    val splitCount = transactions.count { it.myActualShare != it.amount }
                    FilterChip(
                        selected = selectedFilter == TransactionFilter.SPLIT,
                        onClick = { selectedFilter = TransactionFilter.SPLIT },
                        label = { Text("Splits ($splitCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No transactions found in this filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedCategoryFilter != null) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onClearCategoryFilter) {
                                Text("Clear Category Filter", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredList, key = { it.id }) { txn ->
                        TransactionDetailCard(
                            transaction = txn,
                            splits = splitsByTxnId[txn.id] ?: emptyList(),
                            onOpenCategoryPicker = { transactionToCategorize = txn },
                            onOpenSplitDialog = { transactionToSplit = txn },
                            onDelete = { onDelete(txn.id) }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    // Category Picker Dialog
    transactionToCategorize?.let { txn ->
        CategoryPickerDialog(
            transaction = txn,
            categories = categories,
            onDismiss = { transactionToCategorize = null },
            onSelectCategory = { cat, createRule ->
                onCategorize(txn.id, cat, createRule)
                transactionToCategorize = null
            }
        )
    }

    // Split Dialog
    transactionToSplit?.let { txn ->
        SplitExpenseDialog(
            transaction = txn,
            existingSplits = splitsByTxnId[txn.id] ?: emptyList(),
            onDismiss = { transactionToSplit = null },
            onSave = { myShare, splitsList ->
                onSplit(txn.id, myShare, splitsList)
                transactionToSplit = null
            }
        )
    }
}

@Composable
fun TransactionDetailCard(
    transaction: TransactionEntity,
    splits: List<SplitEntryEntity> = emptyList(),
    onOpenCategoryPicker: () -> Unit,
    onOpenSplitDialog: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val isDebit = transaction.direction == TransactionDirection.DEBIT
    val isSplit = transaction.myActualShare != transaction.amount || splits.isNotEmpty()
    val roommatesOwed = maxOf(0.0, transaction.amount - transaction.myActualShare)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Payee info on left, Personal Share on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    MerchantAvatar(
                        merchantName = transaction.cleanMerchant ?: "Unknown Payee",
                        isDebit = isDebit,
                        size = 42.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = transaction.cleanMerchant ?: "Unknown Payee",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = dateFormat.format(Date(transaction.timestampMillis)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (isSplit) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = "Split",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = (if (isDebit) "- " else "+ ") + String.format(Locale.getDefault(), "₹%.2f", transaction.myActualShare),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDebit) RoseDebit else EmeraldSuccess
                    )
                    if (isSplit) {
                        Text(
                            text = "Your Share (Bill: ₹${String.format(Locale.getDefault(), "%.2f", transaction.amount)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Details row: Account + Ref + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listOfNotNull(
                        transaction.accountInfo,
                        transaction.referenceNumber?.let { "Ref: $it" }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = when (transaction.status) {
                        TransactionStatus.AUTO_MATCHED -> EmeraldSuccess.copy(alpha = 0.08f)
                        TransactionStatus.PENDING_REVIEW -> AmberPending.copy(alpha = 0.08f)
                        TransactionStatus.MANUALLY_SET -> PrimaryIndigo.copy(alpha = 0.08f)
                    }
                ) {
                    Text(
                        text = when (transaction.status) {
                            TransactionStatus.AUTO_MATCHED -> "Auto-Matched"
                            TransactionStatus.PENDING_REVIEW -> "Pending"
                            TransactionStatus.MANUALLY_SET -> "Manual"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (transaction.status) {
                            TransactionStatus.AUTO_MATCHED -> EmeraldSuccess
                            TransactionStatus.PENDING_REVIEW -> AmberPending
                            TransactionStatus.MANUALLY_SET -> PrimaryIndigo
                        },
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Dedicated Split Breakdown Box when this expense is split
            if (isSplit) {
                Spacer(Modifier.height(16.dp))
                val yourRatio = if (transaction.amount > 0) (transaction.myActualShare / transaction.amount).toFloat().coerceIn(0f, 1f) else 1f
                val othersRatio = 1f - yourRatio

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Header of split breakdown box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.CallSplit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Split Breakdown",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (roommatesOwed > 0) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = EmeraldSuccess.copy(alpha = 0.08f)
                                ) {
                                    Text(
                                        text = "To Collect: ₹${String.format(Locale.getDefault(), "%.2f", roommatesOwed)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EmeraldSuccess,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // 3 Key Metrics: Total Bill, You Paid, Roommates Portion
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Bill", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    String.format(Locale.getDefault(), "₹%.2f", transaction.amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "You Paid (${(yourRatio * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    String.format(Locale.getDefault(), "₹%.2f", transaction.myActualShare),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(
                                    "Roommates (${(othersRatio * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSuccess
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    String.format(Locale.getDefault(), "₹%.2f", roommatesOwed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Visual proportion bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (yourRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(maxOf(0.01f, yourRatio))
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            if (othersRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(maxOf(0.01f, othersRatio))
                                        .fillMaxHeight()
                                        .background(EmeraldSuccess)
                                )
                            }
                        }

                        // Roommates breakdown if present
                        if (splits.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(12.dp))

                            splits.forEach { split ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = MaterialTheme.shapes.extraSmall,
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = split.roommateName.firstOrNull()?.uppercase() ?: "?",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            split.roommateName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            String.format(Locale.getDefault(), "₹%.2f", split.amountOwed),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Surface(
                                            shape = MaterialTheme.shapes.extraSmall,
                                            color = if (split.settled) EmeraldSuccess.copy(alpha = 0.08f) else AmberPending.copy(alpha = 0.08f)
                                        ) {
                                            Text(
                                                text = if (split.settled) "Settled" else "Unsettled",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (split.settled) EmeraldSuccess else AmberPending,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            // Action row: Category chip, Split button, and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onOpenCategoryPicker,
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (transaction.category != null) {
                                CategoryAvatar(category = transaction.category, size = 20.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = transaction.category,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = "Tap to Categorize",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    leadingIcon = {
                        if (transaction.category == null) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (transaction.category == null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = if (transaction.category == null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        leadingIconContentColor = if (transaction.category == null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    border = null,
                    shape = MaterialTheme.shapes.small
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(
                        onClick = onOpenSplitDialog,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isSplit) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (isSplit) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.CallSplit,
                            contentDescription = "Split Expense",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
