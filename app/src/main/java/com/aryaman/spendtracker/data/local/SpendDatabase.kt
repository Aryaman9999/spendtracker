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
    version = 3,
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

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add comment column to transactions
                db.execSQL("ALTER TABLE transactions ADD COLUMN comment TEXT DEFAULT NULL")

                // 2. Insert new categories for existing installations
                val newCategories = listOf(
                    Triple("Rent", "#B45309", "home"),
                    Triple("Subscriptions", "#7C3AED", "subscriptions"),
                    Triple("EMI & Loans", "#DC2626", "account_balance"),
                    Triple("Investments", "#0D9488", "trending_up"),
                    Triple("Education", "#2563EB", "school"),
                    Triple("Fuel", "#EA580C", "local_gas_station"),
                    Triple("Insurance", "#4338CA", "shield"),
                    Triple("Gifts & Donations", "#DB2777", "card_giftcard")
                )
                for ((name, color, icon) in newCategories) {
                    db.execSQL(
                        "INSERT INTO categories (name, colorHex, iconName) SELECT '$name', '$color', '$icon' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '$name')"
                    )
                }

                // 3. Insert new default rules for subscriptions, investments, fuel
                val newRules = listOf(
                    Triple("netflix", "Subscriptions", 11),
                    Triple("spotify", "Subscriptions", 12),
                    Triple("hotstar", "Subscriptions", 13),
                    Triple("prime", "Subscriptions", 14),
                    Triple("youtube", "Subscriptions", 15),
                    Triple("zerodha", "Investments", 16),
                    Triple("groww", "Investments", 17),
                    Triple("petrol", "Fuel", 18),
                    Triple("fuel", "Fuel", 19),
                    Triple("hpcl", "Fuel", 20),
                    Triple("bpcl", "Fuel", 21),
                    Triple("iocl", "Fuel", 22)
                )
                for ((keyword, category, priority) in newRules) {
                    db.execSQL(
                        "INSERT INTO category_rules (keyword, category, priority, isRegex) SELECT '$keyword', '$category', $priority, 0 WHERE NOT EXISTS (SELECT 1 FROM category_rules WHERE keyword = '$keyword')"
                    )
                }
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
                CategoryEntity(name = "Rent", colorHex = "#B45309", iconName = "home"),
                CategoryEntity(name = "Subscriptions", colorHex = "#7C3AED", iconName = "subscriptions"),
                CategoryEntity(name = "EMI & Loans", colorHex = "#DC2626", iconName = "account_balance"),
                CategoryEntity(name = "Investments", colorHex = "#0D9488", iconName = "trending_up"),
                CategoryEntity(name = "Education", colorHex = "#2563EB", iconName = "school"),
                CategoryEntity(name = "Fuel", colorHex = "#EA580C", iconName = "local_gas_station"),
                CategoryEntity(name = "Insurance", colorHex = "#4338CA", iconName = "shield"),
                CategoryEntity(name = "Gifts & Donations", colorHex = "#DB2777", iconName = "card_giftcard"),
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
                CategoryRuleEntity(keyword = "flipkart", category = "Shopping", priority = 10),
                CategoryRuleEntity(keyword = "netflix", category = "Subscriptions", priority = 11),
                CategoryRuleEntity(keyword = "spotify", category = "Subscriptions", priority = 12),
                CategoryRuleEntity(keyword = "hotstar", category = "Subscriptions", priority = 13),
                CategoryRuleEntity(keyword = "prime", category = "Subscriptions", priority = 14),
                CategoryRuleEntity(keyword = "youtube", category = "Subscriptions", priority = 15),
                CategoryRuleEntity(keyword = "zerodha", category = "Investments", priority = 16),
                CategoryRuleEntity(keyword = "groww", category = "Investments", priority = 17),
                CategoryRuleEntity(keyword = "petrol", category = "Fuel", priority = 18),
                CategoryRuleEntity(keyword = "fuel", category = "Fuel", priority = 19),
                CategoryRuleEntity(keyword = "hpcl", category = "Fuel", priority = 20),
                CategoryRuleEntity(keyword = "bpcl", category = "Fuel", priority = 21),
                CategoryRuleEntity(keyword = "iocl", category = "Fuel", priority = 22)
            )
            db.categoryRuleDao().insertAll(initialRules)
        }
    }
}
