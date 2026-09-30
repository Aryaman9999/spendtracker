package com.aryaman.spendtracker.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Sleek Low-Profile Header (Consistent with Home and Splits)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${filteredList.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            if (selectedCategoryFilter != null) {
                TextButton(
                    onClick = onClearCategoryFilter,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Clear Filter", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                }
            }
        }
            // Active Category Filter Banner (Compact Pill)
            if (selectedCategoryFilter != null) {
                val catColor = getCategoryColor(selectedCategoryFilter)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = catColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryAvatar(category = selectedCategoryFilter, size = 26.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Filtered: $selectedCategoryFilter",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = onClearCategoryFilter,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear Filter",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Sleek Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == TransactionFilter.ALL,
                        onClick = { selectedFilter = TransactionFilter.ALL },
                        label = { Text("All (${transactions.size})") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    val pendingCount = transactions.count { it.status == TransactionStatus.PENDING_REVIEW }
                    FilterChip(
                        selected = selectedFilter == TransactionFilter.PENDING,
                        onClick = { selectedFilter = TransactionFilter.PENDING },
                        label = { Text("Pending ($pendingCount)") },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberContainer,
                            selectedLabelColor = AmberPending
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == TransactionFilter.AUTO_MATCHED,
                        onClick = { selectedFilter = TransactionFilter.AUTO_MATCHED },
                        label = { Text("Auto-Matched") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    val splitCount = transactions.count { it.myActualShare != it.amount }
                    FilterChip(
                        selected = selectedFilter == TransactionFilter.SPLIT,
                        onClick = { selectedFilter = TransactionFilter.SPLIT },
                        label = { Text("Splits ($splitCount)") },
                        shape = RoundedCornerShape(10.dp)
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
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No transactions match this filter",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedCategoryFilter != null) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onClearCategoryFilter) {
                                Text("Clear Category Filter", color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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

/**
 * Compact, scannable transaction item that expands on tap.
 * Eliminates wrapping bugs, bloated nested boxes, and cluttered buttons.
 */
@Composable
fun TransactionDetailCard(
    transaction: TransactionEntity,
    splits: List<SplitEntryEntity> = emptyList(),
    onOpenCategoryPicker: () -> Unit,
    onOpenSplitDialog: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val isDebit = transaction.direction == TransactionDirection.DEBIT
    val isSplit = transaction.myActualShare != transaction.amount || splits.isNotEmpty()
    val roommatesOwed = maxOf(0.0, transaction.amount - transaction.myActualShare)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Main Compact Scannable Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vector Squircle Icon
                MerchantAvatar(
                    merchantName = transaction.cleanMerchant ?: "Unknown Payee",
                    isDebit = isDebit,
                    category = transaction.category,
                    size = 42.dp
                )

                Spacer(Modifier.width(12.dp))

                // Payee & Date (Ellipsized to NEVER wrap onto multi-line mess)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.cleanMerchant ?: "Unknown Payee",
                        style = MaterialTheme.typography.titleMedium,
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

                Spacer(Modifier.width(8.dp))

                // Amount & Split Badge
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

            // Expandable Details (Smoothly revealed ONLY when user taps!)
            if (isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    Spacer(Modifier.height(10.dp))

                    // Account, Reference & Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = listOfNotNull(
                                transaction.accountInfo,
                                transaction.referenceNumber?.let { "Ref: $it" }
                            ).joinToString(" • ").ifEmpty { "Manual Entry" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (transaction.status) {
                                TransactionStatus.AUTO_MATCHED -> EmeraldSuccess.copy(alpha = 0.12f)
                                TransactionStatus.PENDING_REVIEW -> AmberPending.copy(alpha = 0.12f)
                                TransactionStatus.MANUALLY_SET -> PrimaryIndigo.copy(alpha = 0.12f)
                            }
                        ) {
                            Text(
                                text = when (transaction.status) {
                                    TransactionStatus.AUTO_MATCHED -> "Auto-Matched"
                                    TransactionStatus.PENDING_REVIEW -> "Pending Review"
                                    TransactionStatus.MANUALLY_SET -> "Manual"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (transaction.status) {
                                    TransactionStatus.AUTO_MATCHED -> EmeraldSuccess
                                    TransactionStatus.PENDING_REVIEW -> AmberPending
                                    TransactionStatus.MANUALLY_SET -> PrimaryIndigoDark
                                },
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Compact Split Breakdown if split
                    if (isSplit) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Split Summary",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryIndigoDark
                                    )
                                    if (roommatesOwed > 0) {
                                        Text(
                                            text = "Owed to you: ₹${String.format(Locale.getDefault(), "%.2f", roommatesOwed)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldSuccess
                                        )
                                    }
                                }

                                if (splits.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    splits.forEach { split ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "• ${split.roommateName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "₹${String.format(Locale.getDefault(), "%.2f", split.amountOwed)} (${if (split.settled) "Settled" else "Unsettled"})",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (split.settled) EmeraldSuccess else AmberPending
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Actions: Category Chip, Split Button, Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = onOpenCategoryPicker,
                            label = {
                                Text(
                                    text = transaction.category ?: "Tap to Categorize",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                            },
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = onOpenSplitDialog,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Split", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
