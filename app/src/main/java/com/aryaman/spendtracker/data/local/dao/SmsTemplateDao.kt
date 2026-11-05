package com.aryaman.spendtracker.data.local.dao

import androidx.room.*
import com.aryaman.spendtracker.data.local.entity.SmsTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsTemplateDao {

    @Query("SELECT * FROM sms_templates ORDER BY id DESC")
    fun getAllTemplates(): Flow<List<SmsTemplateEntity>>

    @Query("SELECT * FROM sms_templates WHERE isActive = 1 ORDER BY id DESC")
    fun getActiveTemplates(): Flow<List<SmsTemplateEntity>>

    @Query("SELECT * FROM sms_templates WHERE isActive = 1 ORDER BY id DESC")
    suspend fun getActiveTemplatesList(): List<SmsTemplateEntity>

    @Query("SELECT * FROM sms_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: Long): SmsTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: SmsTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(templates: List<SmsTemplateEntity>)

    @Update
    suspend fun update(template: SmsTemplateEntity)

    @Delete
    suspend fun delete(template: SmsTemplateEntity)

    @Query("DELETE FROM sms_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE sms_templates SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)
}
