package com.aryaman.spendtracker.data.repository

import com.aryaman.spendtracker.data.local.dao.CategoryDao
import com.aryaman.spendtracker.data.local.dao.CategoryRuleDao
import com.aryaman.spendtracker.data.local.dao.SmsTemplateDao
import com.aryaman.spendtracker.data.local.dao.SplitEntryDao
import com.aryaman.spendtracker.data.local.dao.TransactionDao
import com.aryaman.spendtracker.data.local.entity.CategoryEntity
import com.aryaman.spendtracker.data.local.entity.CategoryRuleEntity
import com.aryaman.spendtracker.data.local.entity.SmsTemplateEntity
import com.aryaman.spendtracker.data.local.entity.SplitEntryEntity
import com.aryaman.spendtracker.data.local.entity.TransactionEntity
import com.aryaman.spendtracker.data.model.TransactionStatus
import com.aryaman.spendtracker.data.parser.ParsedTransaction
import com.aryaman.spendtracker.domain.RuleEngine
import kotlinx.coroutines.flow.Flow

sealed class TransactionProcessResult {
    data class Inserted(val id: Long, val status: TransactionStatus, val category: String?, val isPendingReview: Boolean) : TransactionProcessResult()
    data class Merged(val id: Long, val reason: String) : TransactionProcessResult()
    data class Ignored(val reason: String) : TransactionProcessResult()
}

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val splitEntryDao: SplitEntryDao,
    private val categoryDao: CategoryDao,
    private val smsTemplateDao: SmsTemplateDao
) {

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val pendingTransactions: Flow<List<TransactionEntity>> = transactionDao.getTransactionsByStatus(TransactionStatus.PENDING_REVIEW)
    val allRules: Flow<List<CategoryRuleEntity>> = categoryRuleDao.getAllRules()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val roommateBalances = splitEntryDao.getRoommateBalances()
    val allSplits: Flow<List<SplitEntryEntity>> = splitEntryDao.getAllSplits()
    val allTemplates: Flow<List<SmsTemplateEntity>> = smsTemplateDao.getAllTemplates()
    val activeTemplates: Flow<List<SmsTemplateEntity>> = smsTemplateDao.getActiveTemplates()

    /**
     * Ingests a ParsedTransaction with automatic deduplication, rule evaluation, and database insertion.
     */
    suspend fun processParsedTransaction(parsed: ParsedTransaction): TransactionProcessResult {
        // 1. Deduplication via Reference / UTR Number
        if (!parsed.referenceNumber.isNullOrBlank()) {
            val existingByRef = transactionDao.findByReferenceNumber(parsed.referenceNumber)
            if (existingByRef != null) {
                // Enrich existing transaction if new record has better merchant details
                val updatedMerchant = if (existingByRef.cleanMerchant.isNullOrBlank() || existingByRef.cleanMerchant == "Unknown") {
                    parsed.cleanMerchant
                } else existingByRef.cleanMerchant

                if (updatedMerchant != existingByRef.cleanMerchant) {
                    transactionDao.update(existingByRef.copy(cleanMerchant = updatedMerchant))
                }
                return TransactionProcessResult.Merged(existingByRef.id, "Matched reference: ${parsed.referenceNumber}")
            }
        }

        // 2. Fuzzy Deduplication: Match within ±90 seconds with identical amount and direction
        val windowMillis = 90_000L
        val duplicateCandidate = transactionDao.findPotentialDuplicate(
            amount = parsed.amount,
            direction = parsed.direction,
            minTimestamp = parsed.timestampMillis - windowMillis,
            maxTimestamp = parsed.timestampMillis + windowMillis
        )

        if (duplicateCandidate != null) {
            // If candidate lacks reference number, enrich it
            val updated = duplicateCandidate.copy(
                referenceNumber = duplicateCandidate.referenceNumber ?: parsed.referenceNumber,
                accountInfo = duplicateCandidate.accountInfo ?: parsed.accountInfo
            )
            transactionDao.update(updated)
            return TransactionProcessResult.Merged(duplicateCandidate.id, "Fuzzy timestamp & amount match")
        }

        // 3. Auto-Categorization via Rule Engine
        val rules = categoryRuleDao.getAllRulesList()
        val ruleMatch = RuleEngine.match(
            merchantText = parsed.cleanMerchant,
            rawText = parsed.rawText,
            rules = rules
        )

        val (status, category, ruleId) = if (ruleMatch != null) {
            Triple(TransactionStatus.AUTO_MATCHED, ruleMatch.category, ruleMatch.ruleId)
        } else {
            Triple(TransactionStatus.PENDING_REVIEW, null, null)
        }

        val isCc = parsed.isCreditCard ||
            parsed.rawText.contains("credit card", ignoreCase = true) ||
            parsed.rawText.contains("pixel play", ignoreCase = true) ||
            parsed.rawText.contains("hdfc card", ignoreCase = true) ||
            parsed.rawText.contains("card ending", ignoreCase = true) ||
            parsed.accountInfo?.contains("Credit Card", ignoreCase = true) == true ||
            parsed.accountInfo?.contains("Pixel", ignoreCase = true) == true ||
            parsed.accountInfo?.contains("CC", ignoreCase = true) == true

        val newEntity = TransactionEntity(
            timestampMillis = parsed.timestampMillis,
            source = parsed.source,
            rawText = parsed.rawText,
            amount = parsed.amount,
            direction = parsed.direction,
            merchantText = parsed.rawMerchant,
            cleanMerchant = parsed.cleanMerchant,
            category = category,
            myActualShare = parsed.amount,
            status = status,
            ruleIdApplied = ruleId,
            referenceNumber = parsed.referenceNumber,
            accountInfo = parsed.accountInfo,
            isCreditCard = isCc,
            isBudgetExempt = false
        )

        val newId = transactionDao.insert(newEntity)

        return TransactionProcessResult.Inserted(
            id = newId,
            status = status,
            category = category,
            isPendingReview = (status == TransactionStatus.PENDING_REVIEW)
        )
    }

    /**
     * Manually categorize a transaction. Optionally creates a persistent rule.
     */
    suspend fun setCategory(transactionId: Long, category: String, createRuleKeyword: String? = null) {
        val transaction = transactionDao.getTransactionById(transactionId) ?: return

        var ruleId: Long? = null
        if (!createRuleKeyword.isNullOrBlank()) {
            val newRule = CategoryRuleEntity(
                keyword = createRuleKeyword.trim().lowercase(),
                category = category,
                priority = 50,
                isRegex = false
            )
            ruleId = categoryRuleDao.insert(newRule)
        }

        transactionDao.update(
            transaction.copy(
                category = category,
                status = TransactionStatus.MANUALLY_SET,
                ruleIdApplied = ruleId ?: transaction.ruleIdApplied
            )
        )
    }

    /**
     * Splits a transaction between user and roommates.
     * Updates myActualShare so dashboard calculations accurately reflect only user's personal portion!
     */
    suspend fun splitTransaction(
        transactionId: Long,
        myActualShare: Double,
        splits: List<SplitEntryEntity>
    ) {
        val transaction = transactionDao.getTransactionById(transactionId) ?: return

        // 1. Update user's personal share
        transactionDao.update(transaction.copy(myActualShare = myActualShare))

        // 2. Replace existing split entries for this transaction
        splitEntryDao.deleteForTransaction(transactionId)
        val validSplits = splits.map { it.copy(transactionId = transactionId) }
        if (validSplits.isNotEmpty()) {
            splitEntryDao.insertAll(validSplits)
        }
    }

    suspend fun updateTransactionComment(transactionId: Long, comment: String?) {
        transactionDao.updateComment(transactionId, comment?.trim()?.ifEmpty { null })
    }

    /**
     * Toggles split status for a transaction:
     * If currently split, reverts to personal (myActualShare = amount, removes split entries).
     * If not split, splits 50/50 with the most recent roommate (or "Roommate" if none).
     */
    suspend fun toggleQuickSplit(transactionId: Long): Boolean {
        val transaction = transactionDao.getTransactionById(transactionId) ?: return false
        val currentSplits = splitEntryDao.getSplitsForTransactionList(transactionId)
        val isCurrentlySplit = transaction.myActualShare != transaction.amount || currentSplits.isNotEmpty()

        if (isCurrentlySplit) {
            transactionDao.update(transaction.copy(myActualShare = transaction.amount))
            splitEntryDao.deleteForTransaction(transactionId)
            return false
        } else {
            val half = transaction.amount / 2.0
            val recentRoommate = splitEntryDao.getMostRecentRoommateName() ?: "Roommate"
            transactionDao.update(transaction.copy(myActualShare = half))
            splitEntryDao.deleteForTransaction(transactionId)
            splitEntryDao.insert(
                SplitEntryEntity(
                    transactionId = transactionId,
                    roommateName = recentRoommate,
                    amountOwed = half,
                    settled = false
                )
            )
            return true
        }
    }

    fun getSplitsForTransaction(transactionId: Long): Flow<List<SplitEntryEntity>> =
        splitEntryDao.getSplitsForTransaction(transactionId)

    suspend fun setSplitSettled(splitId: Long, settled: Boolean) =
        splitEntryDao.setSettledStatus(splitId, settled)

    suspend fun settleAllForRoommate(roommateName: String) =
        splitEntryDao.settleAllForRoommate(roommateName)

    suspend fun addRule(rule: CategoryRuleEntity): Long = categoryRuleDao.insert(rule)
    suspend fun updateRule(rule: CategoryRuleEntity) = categoryRuleDao.update(rule)
    suspend fun deleteRule(rule: CategoryRuleEntity) = categoryRuleDao.delete(rule)

    suspend fun getActiveTemplatesList(): List<SmsTemplateEntity> = smsTemplateDao.getActiveTemplatesList()
    suspend fun addTemplate(template: SmsTemplateEntity): Long = smsTemplateDao.insert(template)
    suspend fun updateTemplate(template: SmsTemplateEntity) = smsTemplateDao.update(template)
    suspend fun deleteTemplate(template: SmsTemplateEntity) = smsTemplateDao.delete(template)
    suspend fun deleteTemplateById(id: Long) = smsTemplateDao.deleteById(id)
    suspend fun setTemplateActive(id: Long, isActive: Boolean) = smsTemplateDao.setActive(id, isActive)

    suspend fun deleteTransaction(id: Long) = transactionDao.deleteById(id)

    suspend fun toggleCreditCard(transactionId: Long): Boolean {
        val transaction = transactionDao.getTransactionById(transactionId) ?: return false
        val newStatus = !transaction.isCreditCard
        transactionDao.updateCreditCard(transactionId, newStatus)
        return newStatus
    }

    suspend fun toggleBudgetExempt(transactionId: Long): Boolean {
        val transaction = transactionDao.getTransactionById(transactionId) ?: return false
        val newStatus = !transaction.isBudgetExempt
        transactionDao.updateBudgetExempt(transactionId, newStatus)
        return newStatus
    }

    suspend fun updateCategoryBudget(categoryName: String, budget: Double?) {
        categoryDao.updateMonthlyBudgetByName(categoryName, budget)
    }

    fun getBudgetSpendByCategory(start: Long, end: Long) = transactionDao.getBudgetSpendByCategory(start, end)
    fun getSpendByCategory(start: Long, end: Long) = transactionDao.getSpendByCategory(start, end)
    fun getTotalSpend(start: Long, end: Long) = transactionDao.getTotalSpendInWindow(start, end)
}
