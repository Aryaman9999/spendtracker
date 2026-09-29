package com.aryaman.spendtracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.aryaman.spendtracker.data.local.SpendDatabase
import com.aryaman.spendtracker.data.repository.TransactionRepository

class SpendTrackerApp : Application() {

    lateinit var database: SpendDatabase
        private set

    lateinit var repository: TransactionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = SpendDatabase.getInstance(this)
        repository = TransactionRepository(
            transactionDao = database.transactionDao(),
            categoryRuleDao = database.categoryRuleDao(),
            splitEntryDao = database.splitEntryDao(),
            categoryDao = database.categoryDao(),
            smsTemplateDao = database.smsTemplateDao()
        )

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_REVIEW_QUEUE,
                "Review Uncategorized Spends",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you when an unknown expense requires categorization"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_REVIEW_QUEUE = "spend_review_queue"

        lateinit var instance: SpendTrackerApp
            private set
    }
}
