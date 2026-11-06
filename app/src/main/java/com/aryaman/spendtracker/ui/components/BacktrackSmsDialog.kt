package com.aryaman.spendtracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryaman.spendtracker.data.local.entity.SmsTemplateEntity
import com.aryaman.spendtracker.data.repository.SmsBacktrackSummary
import com.aryaman.spendtracker.ui.theme.AmberPending
import com.aryaman.spendtracker.ui.theme.EmeraldSuccess
import com.aryaman.spendtracker.ui.theme.PrimaryIndigo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacktrackSmsDialog(
    templates: List<SmsTemplateEntity>,
    initialTemplateId: Long? = null,
    isScanning: Boolean,
    scanProgress: Pair<Int, Int>?, // scanned, matched
    scanSummary: SmsBacktrackSummary?,
    onDismiss: () -> Unit,
    onStartScan: (lookbackDays: Int, templateId: Long?) -> Unit,
    onViewTransactions: () -> Unit = {}
) {
    var daysText by remember { mutableStateOf("30") }
    var selectedTemplateId by remember { mutableStateOf(initialTemplateId) }

    AlertDialog(
        onDismissRequest = {
            if (!isScanning) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (scanSummary != null) Icons.Default.CheckCircle else Icons.Default.History,
                    contentDescription = null,
                    tint = if (scanSummary != null) EmeraldSuccess else PrimaryIndigo,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = when {
                        scanSummary != null -> "Scan Complete"
                        isScanning -> "Scanning Past SMS..."
                        else -> "Scan Past Transactions"
                    },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (scanSummary != null) {
                    // Result Summary Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Scan Results",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("SMS Examined:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${scanSummary.totalSmsExamined}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Format Matches:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${scanSummary.matchedTransactions}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Newly Imported:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                                Text("${scanSummary.newlyInserted}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = EmeraldSuccess)
                            }

                            if (scanSummary.newlyInserted > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• Auto-categorized by rules:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${scanSummary.autoCategorized}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• Placed in Uncategorized:", style = MaterialTheme.typography.labelSmall, color = AmberPending)
                                    Text("${scanSummary.uncategorized}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = AmberPending)
                                }
                            }

                            if (scanSummary.duplicatesOrMerged > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Duplicates skipped:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${scanSummary.duplicatesOrMerged}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    if (scanSummary.newlyInserted == 0 && scanSummary.matchedTransactions == 0) {
                        Text(
                            text = "No matching bank messages found in your SMS inbox for the selected lookback period.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (scanSummary.uncategorized > 0) {
                        Text(
                            text = "💡 ${scanSummary.uncategorized} transactions are placed under Uncategorized (Pending). You can categorize or split them on the Spends page.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (isScanning) {
                    // Scanning In Progress
                    Text(
                        text = "Analyzing messages in your SMS inbox against active bank formats...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    scanProgress?.let { (scanned, matched) ->
                        Text(
                            text = "Examined $scanned SMS messages • Found $matched transactions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    // Configuration Form
                    Text(
                        text = "Scans your SMS inbox for past transactions. Messages matching your rules are categorized automatically; all others are placed under Uncategorized.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Target Template Selection (if multiple)
                    if (templates.isNotEmpty() && initialTemplateId == null) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Formats to apply:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (selectedTemplateId != null) {
                                        templates.firstOrNull { it.id == selectedTemplateId }?.bankName ?: "Selected Format"
                                    } else {
                                        "All Active Bank Formats (${templates.size})"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Lookback Days Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Lookback Period:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = daysText,
                            onValueChange = { daysText = it.filter { ch -> ch.isDigit() }.take(3) },
                            label = { Text("Number of days") },
                            placeholder = { Text("30") },
                            suffix = { Text("days") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick Preset Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            listOf("7", "14", "30", "60", "90").forEach { d ->
                                FilterChip(
                                    selected = daysText == d,
                                    onClick = { daysText = d },
                                    label = { Text("${d}d", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (scanSummary != null) {
                if (scanSummary.newlyInserted > 0) {
                    Button(onClick = {
                        onDismiss()
                        onViewTransactions()
                    }) {
                        Text("Review Transactions", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            } else if (!isScanning) {
                Button(
                    onClick = {
                        val days = daysText.toIntOrNull() ?: 30
                        onStartScan(days, selectedTemplateId)
                    },
                    enabled = (daysText.toIntOrNull() ?: 0) > 0
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Scan & Import", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isScanning && scanSummary == null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            } else if (scanSummary != null && scanSummary.newlyInserted > 0) {
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        }
    )
}
