package com.aryaman.spendtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aryaman.spendtracker.data.local.entity.CategoryRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {

    @Query("SELECT * FROM category_rules ORDER BY priority ASC, id ASC")
    fun getAllRules(): Flow<List<CategoryRuleEntity>>

    @Query("SELECT * FROM category_rules ORDER BY priority ASC, id ASC")
    suspend fun getAllRulesList(): List<CategoryRuleEntity>

    @Query("SELECT * FROM category_rules WHERE id = :id LIMIT 1")
    suspend fun getRuleById(id: Long): CategoryRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CategoryRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<CategoryRuleEntity>)

    @Update
    suspend fun update(rule: CategoryRuleEntity)

    @Delete
    suspend fun delete(rule: CategoryRuleEntity)

    @Query("DELETE FROM category_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
