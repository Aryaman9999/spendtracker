package com.aryaman.spendtracker.data.parser

import com.aryaman.spendtracker.data.model.TransactionDirection
import com.aryaman.spendtracker.data.model.TransactionSource

/**
 * Clean data class representing extracted details from SMS or Push Notification.
 * Completely decoupled from Android Framework for 100% unit-testability.
 */
data class ParsedTransaction(
    val amount: Double,
    val direction: TransactionDirection,
    val rawMerchant: String,
    val cleanMerchant: String,
    val accountInfo: String? = null,
    val referenceNumber: String? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
    val source: TransactionSource = TransactionSource.SMS,
    val rawText: String
)
