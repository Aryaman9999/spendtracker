package com.aryaman.spendtracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryaman.spendtracker.ui.theme.PrimaryIndigo
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetCategoryBudgetDialog(
    categoryName: String,
    currentBudget: Double?,
    onDismiss: () -> Unit,
    onSaveBudget: (Double?) -> Unit
) {
    var budgetText by remember {
        mutableStateOf(currentBudget?.let { String.format(Locale.getDefault(), "%.0f", it) } ?: "")
    }

    val presets = listOf(2000, 5000, 10000, 15000, 20000, 30000)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Monthly Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Set a maximum monthly spending target for this category. Progress bars on your dashboard will track your utilization.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            budgetText = input
                        }
                    },
                    label = { Text("Budget Limit (₹)") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { amount ->
                        SuggestionChip(
                            onClick = { budgetText = amount.toString() },
                            label = { Text("₹$amount", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = budgetText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        onSaveBudget(amount)
                    } else {
                        onSaveBudget(null)
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Limit")
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentBudget != null && currentBudget > 0) {
                    TextButton(
                        onClick = {
                            onSaveBudget(null)
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove Limit", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
