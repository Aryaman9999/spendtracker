package com.aryaman.spendtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aryaman.spendtracker.data.local.entity.SplitEntryEntity
import kotlinx.coroutines.flow.Flow

data class RoommateBalance(
    val roommateName: String,
    val totalUnsettled: Double,
    val settledCount: Int,
    val unsettledCount: Int
)

@Dao
interface SplitEntryDao {

    @Query("SELECT * FROM split_entries WHERE transactionId = :transactionId")
    fun getSplitsForTransaction(transactionId: Long): Flow<List<SplitEntryEntity>>

    @Query("SELECT * FROM split_entries WHERE transactionId = :transactionId")
    suspend fun getSplitsForTransactionList(transactionId: Long): List<SplitEntryEntity>

    @Query("SELECT * FROM split_entries ORDER BY id DESC")
    fun getAllSplits(): Flow<List<SplitEntryEntity>>

    @Query("SELECT roommateName FROM split_entries ORDER BY id DESC LIMIT 1")
    suspend fun getMostRecentRoommateName(): String?

    @Query("""
        SELECT 
            roommateName,
            SUM(CASE WHEN settled = 0 THEN amountOwed ELSE 0.0 END) as totalUnsettled,
            SUM(CASE WHEN settled = 1 THEN 1 ELSE 0 END) as settledCount,
            SUM(CASE WHEN settled = 0 THEN 1 ELSE 0 END) as unsettledCount
        FROM split_entries 
        GROUP BY roommateName 
        ORDER BY totalUnsettled DESC
    """)
    fun getRoommateBalances(): Flow<List<RoommateBalance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(split: SplitEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(splits: List<SplitEntryEntity>)

    @Update
    suspend fun update(split: SplitEntryEntity)

    @Query("UPDATE split_entries SET settled = :settled WHERE id = :splitId")
    suspend fun setSettledStatus(splitId: Long, settled: Boolean)

    @Query("UPDATE split_entries SET settled = 1 WHERE roommateName = :roommateName")
    suspend fun settleAllForRoommate(roommateName: String)

    @Delete
    suspend fun delete(split: SplitEntryEntity)

    @Query("DELETE FROM split_entries WHERE transactionId = :transactionId")
    suspend fun deleteForTransaction(transactionId: Long)
}
