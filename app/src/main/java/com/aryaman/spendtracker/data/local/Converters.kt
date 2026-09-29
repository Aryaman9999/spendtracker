package com.aryaman.spendtracker.data.local

import androidx.room.TypeConverter
import com.aryaman.spendtracker.data.model.TransactionDirection
import com.aryaman.spendtracker.data.model.TransactionSource
import com.aryaman.spendtracker.data.model.TransactionStatus

class Converters {

    @TypeConverter
    fun fromDirection(direction: TransactionDirection): String = direction.name

    @TypeConverter
    fun toDirection(value: String): TransactionDirection =
        try { TransactionDirection.valueOf(value) } catch (_: Exception) { TransactionDirection.DEBIT }

    @TypeConverter
    fun fromSource(source: TransactionSource): String = source.name

    @TypeConverter
    fun toSource(value: String): TransactionSource =
        try { TransactionSource.valueOf(value) } catch (_: Exception) { TransactionSource.SMS }

    @TypeConverter
    fun fromStatus(status: TransactionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): TransactionStatus =
        try { TransactionStatus.valueOf(value) } catch (_: Exception) { TransactionStatus.PENDING_REVIEW }
}
