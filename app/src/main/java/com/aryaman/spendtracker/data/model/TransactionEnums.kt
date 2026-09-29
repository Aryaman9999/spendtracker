package com.aryaman.spendtracker.data.model

enum class TransactionDirection {
    DEBIT,
    CREDIT
}

enum class TransactionSource {
    SMS,
    NOTIFICATION,
    MANUAL
}

enum class TransactionStatus {
    AUTO_MATCHED,
    MANUALLY_SET,
    PENDING_REVIEW
}
