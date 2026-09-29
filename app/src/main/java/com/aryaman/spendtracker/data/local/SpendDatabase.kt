package com.aryaman.spendtracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryRuleEntity::class,
        SplitEntryEntity::class,
        CategoryEntity::class,
        SmsTemplateEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SpendDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun splitEntryDao(): SplitEntryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun smsTemplateDao(): SmsTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: SpendDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sms_templates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bankName` TEXT NOT NULL,
                        `senderFilter` TEXT,
                        `regexPattern` TEXT NOT NULL,
                        `direction` TEXT NOT NULL,
                        `sampleText` TEXT NOT NULL,
                        `categoryHint` TEXT,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): SpendDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpendDatabase::class.java,
                    "spend_tracker.db"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate default categories and common default rules
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            prepopulateDefaults(database)
                        }
                    }
                })
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun prepopulateDefaults(db: SpendDatabase) {
            val defaultCategories = listOf(
                CategoryEntity(name = "Personal Spend", colorHex = "#8B5CF6", iconName = "person"),
                CategoryEntity(name = "Food & Dining", colorHex = "#FF5722", iconName = "restaurant"),
                CategoryEntity(name = "Groceries", colorHex = "#4CAF50", iconName = "shopping_cart"),
                CategoryEntity(name = "Travel & Commute", colorHex = "#2196F3", iconName = "directions_car"),
                CategoryEntity(name = "Shopping", colorHex = "#9C27B0", iconName = "shopping_bag"),
                CategoryEntity(name = "Bills & Utilities", colorHex = "#FF9800", iconName = "receipt"),
                CategoryEntity(name = "Entertainment", colorHex = "#E91E63", iconName = "movie"),
                CategoryEntity(name = "Health & Medical", colorHex = "#00BCD4", iconName = "local_hospital"),
                CategoryEntity(name = "Other", colorHex = "#607D8B", iconName = "more_horiz")
            )
            db.categoryDao().insertAll(defaultCategories)

            val initialRules = listOf(
                CategoryRuleEntity(keyword = "zomato", category = "Food & Dining", priority = 1),
                CategoryRuleEntity(keyword = "swiggy", category = "Food & Dining", priority = 2),
                CategoryRuleEntity(keyword = "uber", category = "Travel & Commute", priority = 3),
                CategoryRuleEntity(keyword = "ola", category = "Travel & Commute", priority = 4),
                CategoryRuleEntity(keyword = "rapido", category = "Travel & Commute", priority = 5),
                CategoryRuleEntity(keyword = "blinkit", category = "Groceries", priority = 6),
                CategoryRuleEntity(keyword = "zepto", category = "Groceries", priority = 7),
                CategoryRuleEntity(keyword = "instamart", category = "Groceries", priority = 8),
                CategoryRuleEntity(keyword = "amazon", category = "Shopping", priority = 9),
                CategoryRuleEntity(keyword = "flipkart", category = "Shopping", priority = 10)
            )
            db.categoryRuleDao().insertAll(initialRules)
        }
    }
}
