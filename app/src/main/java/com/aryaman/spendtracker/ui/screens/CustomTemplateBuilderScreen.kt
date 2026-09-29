package com.aryaman.spendtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryaman.spendtracker.data.local.entity.SmsTemplateEntity
import com.aryaman.spendtracker.data.model.TransactionDirection
import com.aryaman.spendtracker.data.parser.DynamicTemplateParser
import com.aryaman.spendtracker.data.parser.ParsedTransaction
import com.aryaman.spendtracker.data.parser.SynthesisResult
import com.aryaman.spendtracker.data.parser.TemplateSynthesizer
import com.aryaman.spendtracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTemplateBuilderScreen(
    onBack: () -> Unit,
    onSaveTemplate: (SmsTemplateEntity) -> Unit
) {
    var bankName by remember { mutableStateOf("") }
    var senderFilter by remember { mutableStateOf("") }
    var sampleText by remember { mutableStateOf("") }

    var amountSnippet by remember { mutableStateOf("") }
    var merchantSnippet by remember { mutableStateOf("") }
    var dateSnippet by remember { mutableStateOf("") }
    var timeSnippet by remember { mutableStateOf("") }
    var accountSnippet by remember { mutableStateOf("") }
    var refSnippet by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(TransactionDirection.DEBIT) }

    // Test second SMS state
    var testSmsText by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf(false) }

    // Run synthesis automatically whenever the inputs change
    val synthesisResult by remember(
        sampleText,
        amountSnippet,
        merchantSnippet,
        dateSnippet,
        timeSnippet,
        accountSnippet,
        refSnippet,
        direction,
        bankName,
        senderFilter
    ) {
        derivedStateOf {
            if (sampleText.isBlank() || amountSnippet.isBlank() || merchantSnippet.isBlank()) {
                null
            } else {
                TemplateSynthesizer.synthesize(
                    sampleText = sampleText,
                    amountSnippet = amountSnippet,
                    merchantSnippet = merchantSnippet,
                    dateSnippet = dateSnippet.takeIf { it.isNotBlank() },
                    timeSnippet = timeSnippet.takeIf { it.isNotBlank() },
                    accountSnippet = accountSnippet.takeIf { it.isNotBlank() },
                    refSnippet = refSnippet.takeIf { it.isNotBlank() },
                    direction = direction,
                    bankName = bankName.ifBlank { "Custom Bank" },
                    senderFilter = senderFilter.takeIf { it.isNotBlank() }
                )
            }
        }
    }

    fun applyAutoDetect(text: String) {
        val detected = TemplateSynthesizer.autoDetectSlots(text)
        detected.amount?.let { amountSnippet = it }
        detected.merchant?.let { merchantSnippet = it }
        detected.date?.let { dateSnippet = it }
        detected.time?.let { timeSnippet = it }
        detected.account?.let { accountSnippet = it }
        detected.ref?.let { refSnippet = it }
        direction = detected.direction
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Bank SMS Format", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Explanation Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = PrimaryIndigo.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Paste a real SMS from your bank. SpendTracker will auto-detect the details and create a pattern to parse all future spends from this bank automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Section 1: Bank Information
            Text("1. Bank Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Bank / Card Label (e.g. ICICI Credit Card)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            OutlinedTextField(
                value = senderFilter,
                onValueChange = { senderFilter = it },
                label = { Text("Sender Filter (Optional, e.g. ICICI, AXIS)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Text("Leave blank to match messages from any sender with this format.")
                }
            )

            HorizontalDivider()

            // Section 2: Sample Bank SMS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("2. Sample Bank SMS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                if (sampleText.isNotBlank()) {
                    TextButton(onClick = { applyAutoDetect(sampleText) }) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Auto-Detect")
                    }
                }
            }

            OutlinedTextField(
                value = sampleText,
                onValueChange = {
                    sampleText = it
                    if (amountSnippet.isBlank() && merchantSnippet.isBlank()) {
                        applyAutoDetect(it)
                    }
                },
                label = { Text("Paste Bank SMS Here") },
                placeholder = { Text("e.g. A transaction of Rs. 179.49 was made using your HDFC Bank Credit Card at Zomato...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )

            HorizontalDivider()

            // Section 3: Tagged Slots
            Text("3. Tagged Fields", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Direction Choice
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = direction == TransactionDirection.DEBIT,
                    onClick = { direction = TransactionDirection.DEBIT },
                    label = { Text("🔴 Debit / Spent") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = direction == TransactionDirection.CREDIT,
                    onClick = { direction = TransactionDirection.CREDIT },
                    label = { Text("🟢 Credit / Received") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Amount and Merchant (Required)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountSnippet,
                    onValueChange = { amountSnippet = it },
                    label = { Text("Amount *") },
                    placeholder = { Text("179.49") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = merchantSnippet,
                    onValueChange = { merchantSnippet = it },
                    label = { Text("Payee / Merchant *") },
                    placeholder = { Text("Zomato") },
                    modifier = Modifier.weight(1.3f),
                    singleLine = true
                )
            }

            // Date and Time (Optional)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = dateSnippet,
                    onValueChange = { dateSnippet = it },
                    label = { Text("Date in SMS") },
                    placeholder = { Text("26/09/26") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = timeSnippet,
                    onValueChange = { timeSnippet = it },
                    label = { Text("Time in SMS") },
                    placeholder = { Text("19:44 (Optional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Helper note for time handling
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🕒", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (timeSnippet.isNotBlank()) "Time is specified in the text. SpendTracker will use this exact time."
                        else "No time in SMS text? SpendTracker will automatically use the exact moment the SMS arrived on your phone.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Card / Account & Ref (Optional)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = accountSnippet,
                    onValueChange = { accountSnippet = it },
                    label = { Text("Card / Account (Opt)") },
                    placeholder = { Text("*5164") },
                    modifier = Modifier.weight(1.2f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = refSnippet,
                    onValueChange = { refSnippet = it },
                    label = { Text("Ref / UTR (Opt)") },
                    placeholder = { Text("626963138975") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            HorizontalDivider()

            // Section 4: Live Preview
            Text("4. Live Transaction Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            when (val result = synthesisResult) {
                null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Enter Amount and Payee to generate preview.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is SynthesisResult.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(10.dp))
                            Text(result.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                is SynthesisResult.Success -> {
                    PreviewTransactionItem(preview = result.preview, hasExplicitTime = timeSnippet.isNotBlank())
                }
            }

            // Section 5: Optional Verification with 2nd SMS
            if (synthesisResult is SynthesisResult.Success) {
                HorizontalDivider()

                Text("5. Test with Another Message (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = testSmsText,
                    onValueChange = {
                        testSmsText = it
                        testResult = null
                    },
                    label = { Text("Paste a 2nd SMS from this bank to test") },
                    placeholder = { Text("Verify that variations in amount, merchant, or date work seamlessly.") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            val pattern = (synthesisResult as SynthesisResult.Success).regexPattern
                            val testTemplate = SmsTemplateEntity(
                                bankName = bankName.ifBlank { "Custom Bank" },
                                senderFilter = senderFilter.takeIf { it.isNotBlank() },
                                regexPattern = pattern,
                                direction = direction,
                                sampleText = sampleText
                            )
                            val parsed = DynamicTemplateParser.parseSingle(testSmsText, System.currentTimeMillis(), testTemplate)
                            if (parsed != null) {
                                testSuccess = true
                                testResult = "✓ Success! Parsed ₹${String.format(Locale.getDefault(), "%.2f", parsed.amount)} to ${parsed.cleanMerchant}"
                            } else {
                                testSuccess = false
                                testResult = "⚠️ Failed to match this second message. Please ensure the structure is identical."
                            }
                        },
                        enabled = testSmsText.isNotBlank()
                    ) {
                        Text("Verify Pattern")
                    }

                    testResult?.let { msg ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (testSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (testSuccess) EmeraldSuccess else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (testSuccess) EmeraldSuccess else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    val pattern = (synthesisResult as? SynthesisResult.Success)?.regexPattern ?: return@Button
                    val template = SmsTemplateEntity(
                        bankName = bankName.ifBlank { "Custom Bank" },
                        senderFilter = senderFilter.trim().takeIf { it.isNotBlank() },
                        regexPattern = pattern,
                        direction = direction,
                        sampleText = sampleText.trim()
                    )
                    onSaveTemplate(template)
                },
                enabled = synthesisResult is SynthesisResult.Success && bankName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Bank Format", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun PreviewTransactionItem(preview: ParsedTransaction, hasExplicitTime: Boolean) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val isDebit = preview.direction == TransactionDirection.DEBIT
    val cardBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, EmeraldSuccess.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text("📦", fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = preview.cleanMerchant,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = dateFormat.format(Date(preview.timestampMillis)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = (if (isDebit) "- " else "+ ") + String.format(Locale.getDefault(), "₹%.2f", preview.amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDebit) RoseDebit else EmeraldSuccess
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listOfNotNull(
                        preview.accountInfo,
                        preview.referenceNumber?.let { "Ref: $it" }
                    ).joinToString(" • ").ifBlank { "No Account Details" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldContainer
                ) {
                    Text(
                        text = if (hasExplicitTime) "✓ Pattern Ready" else "🕒 Using SMS receipt time",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
