package com.aryaman.spendtracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aryaman.spendtracker.data.model.TransactionDirection
import com.aryaman.spendtracker.data.model.TransactionSource
import com.aryaman.spendtracker.data.model.TransactionStatus

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["referenceNumber"]),
        Index(value = ["timestampMillis"]),
        Index(value = ["status"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMillis: Long,
    val source: TransactionSource,
    val rawText: String,
    val amount: Double,
    val direction: TransactionDirection,
    val merchantText: String?,
    val cleanMerchant: String?,
    val category: String? = null,
    val myActualShare: Double = amount,
    val status: TransactionStatus = TransactionStatus.PENDING_REVIEW,
    val ruleIdApplied: Long? = null,
    val referenceNumber: String? = null,
    val accountInfo: String? = null
)
