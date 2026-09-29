package com.aryaman.spendtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryaman.spendtracker.data.local.entity.SplitEntryEntity
import com.aryaman.spendtracker.data.local.entity.TransactionEntity
import com.aryaman.spendtracker.ui.theme.EmeraldSuccess
import com.aryaman.spendtracker.ui.theme.RoseDebit
import java.util.Locale

@Composable
fun SplitExpenseDialog(
    transaction: TransactionEntity,
    existingSplits: List<SplitEntryEntity>,
    onDismiss: () -> Unit,
    onSave: (myActualShare: Double, splits: List<SplitEntryEntity>) -> Unit
) {
    var myShareText by remember { mutableStateOf(String.format(Locale.US, "%.2f", transaction.myActualShare)) }

    // Mutable list of roommate entries: Pair(Name, Amount)
    val roommateList = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            if (existingSplits.isNotEmpty()) {
                existingSplits.forEach { add(Pair(it.roommateName, String.format(Locale.US, "%.2f", it.amountOwed))) }
            }
        }
    }

    val myShareValue = myShareText.toDoubleOrNull() ?: 0.0
    val roommatesSum = roommateList.sumOf { it.second.toDoubleOrNull() ?: 0.0 }
    val totalAllocated = myShareValue + roommatesSum
    val difference = transaction.amount - totalAllocated
    val isBalanced = kotlin.math.abs(difference) < 0.05

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = "Split Expense",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total Bill: ₹${String.format(Locale.US, "%.2f", transaction.amount)} • ${transaction.cleanMerchant ?: "Merchant"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick split shortcuts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        onClick = {
                            val half = transaction.amount / 2.0
                            myShareText = String.format(Locale.US, "%.2f", half)
                            roommateList.clear()
                            roommateList.add(Pair("Roommate", String.format(Locale.US, "%.2f", half)))
                        }
                    ) {
                        Text("½ Half", fontWeight = FontWeight.SemiBold)
                    }
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        onClick = {
                            val third = transaction.amount / 3.0
                            myShareText = String.format(Locale.US, "%.2f", third)
                            roommateList.clear()
                            roommateList.add(Pair("Roommate 1", String.format(Locale.US, "%.2f", third)))
                            roommateList.add(Pair("Roommate 2", String.format(Locale.US, "%.2f", third)))
                        }
                    ) {
                        Text("⅓ Third", fontWeight = FontWeight.SemiBold)
                    }
                }

                // User's personal share input
                OutlinedTextField(
                    value = myShareText,
                    onValueChange = { myShareText = it },
                    label = { Text("My Personal Share (₹)") },
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Roommates section
                Text(
                    text = "Roommates who owe you:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                roommateList.forEachIndexed { index, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pair.first,
                            onValueChange = { newName -> roommateList[index] = Pair(newName, pair.second) },
                            label = { Text("Name") },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = pair.second,
                            onValueChange = { newAmt -> roommateList[index] = Pair(pair.first, newAmt) },
                            label = { Text("₹ Owed") },
                            shape = MaterialTheme.shapes.small,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = { roommateList.removeAt(index) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                TextButton(
                    onClick = { roommateList.add(Pair("", "")) },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Roommate", fontWeight = FontWeight.SemiBold)
                }

                // Balance summary box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = if (isBalanced) EmeraldSuccess.copy(alpha = 0.08f) else RoseDebit.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBalanced) "✓ Fully Allocated" else "Difference: ₹${String.format(Locale.US, "%.2f", difference)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isBalanced) EmeraldSuccess else RoseDebit
                        )
                        Text(
                            text = "Total: ₹${String.format(Locale.US, "%.2f", totalAllocated)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val myShare = myShareText.toDoubleOrNull() ?: transaction.amount
                    val splits = roommateList.mapNotNull {
                        val name = it.first.trim()
                        val amt = it.second.toDoubleOrNull()
                        if (name.isNotEmpty() && amt != null && amt > 0) {
                            SplitEntryEntity(
                                transactionId = transaction.id,
                                roommateName = name,
                                amountOwed = amt,
                                settled = false
                            )
                        } else null
                    }
                    onSave(myShare, splits)
                },
                shape = MaterialTheme.shapes.small
            ) {
                Text("Save Split", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = MaterialTheme.shapes.small
            ) {
                Text("Cancel")
            }
        }
    )
}
