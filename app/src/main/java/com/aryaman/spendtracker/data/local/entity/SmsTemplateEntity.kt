package com.aryaman.spendtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aryaman.spendtracker.data.model.TransactionDirection

@Entity(tableName = "sms_templates")
data class SmsTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String,
    val senderFilter: String? = null,
    val regexPattern: String,
    val direction: TransactionDirection = TransactionDirection.DEBIT,
    val sampleText: String,
    val categoryHint: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
