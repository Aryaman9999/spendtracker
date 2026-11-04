package com.aryaman.spendtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aryaman.spendtracker.data.local.entity.TransactionEntity
import com.aryaman.spendtracker.data.model.TransactionDirection
import com.aryaman.spendtracker.data.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

data class CategorySpendSummary(
    val category: String,
    val totalAmount: Double
)

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestampMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY timestampMillis DESC")
    fun getTransactionsByStatus(status: TransactionStatus): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("""
        SELECT * FROM transactions 
        WHERE referenceNumber IS NOT NULL AND referenceNumber = :referenceNumber 
        LIMIT 1
    """)
    suspend fun findByReferenceNumber(referenceNumber: String): TransactionEntity?

    /**
     * Finds potential duplicates within a window (e.g. ±90 seconds) matching amount and direction.
     * Useful when UPI notification and Bank SMS fire for the same transaction.
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE amount = :amount 
          AND direction = :direction 
          AND timestampMillis BETWEEN :minTimestamp AND :maxTimestamp 
        ORDER BY timestampMillis DESC 
        LIMIT 1
    """)
    suspend fun findPotentialDuplicate(
        amount: Double,
        direction: TransactionDirection,
        minTimestamp: Long,
        maxTimestamp: Long
    ): TransactionEntity?

    @Query("""
        SELECT category, SUM(myActualShare) as totalAmount 
        FROM transactions 
        WHERE direction = 'DEBIT' 
          AND timestampMillis BETWEEN :startTimestamp AND :endTimestamp 
          AND category IS NOT NULL
        GROUP BY category 
        ORDER BY totalAmount DESC
    """)
    fun getSpendByCategory(startTimestamp: Long, endTimestamp: Long): Flow<List<CategorySpendSummary>>

    /**
     * Category spend specifically counting toward budget (excludes transactions marked isBudgetExempt).
     */
    @Query("""
        SELECT category, SUM(myActualShare) as totalAmount 
        FROM transactions 
        WHERE direction = 'DEBIT' 
          AND timestampMillis BETWEEN :startTimestamp AND :endTimestamp 
          AND category IS NOT NULL
          AND isBudgetExempt = 0
        GROUP BY category 
        ORDER BY totalAmount DESC
    """)
    fun getBudgetSpendByCategory(startTimestamp: Long, endTimestamp: Long): Flow<List<CategorySpendSummary>>

    /**
     * Top categories in time window synchronously, for the homescreen widget.
     */
    @Query("""
        SELECT category, SUM(myActualShare) as totalAmount 
        FROM transactions 
        WHERE direction = 'DEBIT' 
          AND timestampMillis BETWEEN :startTimestamp AND :endTimestamp 
          AND category IS NOT NULL
        GROUP BY category 
        ORDER BY totalAmount DESC 
        LIMIT :limit
    """)
    suspend fun getTopCategoriesInWindowSync(startTimestamp: Long, endTimestamp: Long, limit: Int = 5): List<CategorySpendSummary>

    @Query("""
        SELECT SUM(myActualShare) 
        FROM transactions 
        WHERE direction = 'DEBIT' 
          AND timestampMillis BETWEEN :startTimestamp AND :endTimestamp
    """)
    fun getTotalSpendInWindow(startTimestamp: Long, endTimestamp: Long): Flow<Double?>

    /** Synchronous version for widget updates (runs on IO thread). */
    @Query("""
        SELECT SUM(myActualShare) 
        FROM transactions 
        WHERE direction = 'DEBIT' 
          AND timestampMillis BETWEEN :startTimestamp AND :endTimestamp
    """)
    suspend fun getTotalSpendInWindowSync(startTimestamp: Long, endTimestamp: Long): Double?

    /** Get the N most recent DEBIT transactions, synchronously (for widget). */
    @Query("""
        SELECT * FROM transactions 
        WHERE direction = 'DEBIT' 
        ORDER BY timestampMillis DESC 
        LIMIT :limit
    """)
    suspend fun getRecentDebitsSync(limit: Int): List<TransactionEntity>

    @Query("UPDATE transactions SET comment = :comment WHERE id = :id")
    suspend fun updateComment(id: Long, comment: String?)

    @Query("UPDATE transactions SET isCreditCard = :isCreditCard WHERE id = :id")
    suspend fun updateCreditCard(id: Long, isCreditCard: Boolean)

    @Query("UPDATE transactions SET isBudgetExempt = :exempt WHERE id = :id")
    suspend fun updateBudgetExempt(id: Long, exempt: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
