package com.aryaman.spendtracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.aryaman.spendtracker.SpendTrackerApp
import com.aryaman.spendtracker.widget.SpendWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CATEGORIZE) {
            val txnId = intent.getLongExtra(EXTRA_TXN_ID, -1L)
            val category = intent.getStringExtra(EXTRA_CATEGORY)
            val merchant = intent.getStringExtra(EXTRA_MERCHANT)

            if (txnId != -1L && !category.isNullOrBlank()) {
                val repository = (context.applicationContext as SpendTrackerApp).repository

                CoroutineScope(Dispatchers.IO).launch {
                    // Update transaction and optionally create rule for merchant
                    repository.setCategory(
                        transactionId = txnId,
                        category = category,
                        createRuleKeyword = merchant
                    )
                    SpendWidgetProvider.refreshAllWidgets(context)
                }

                // Dismiss the notification
                NotificationManagerCompat.from(context).cancel(txnId.toInt())
            }
        } else if (intent.action == ACTION_SPLIT) {
            val txnId = intent.getLongExtra(EXTRA_TXN_ID, -1L)
            if (txnId != -1L) {
                val repository = (context.applicationContext as SpendTrackerApp).repository
                CoroutineScope(Dispatchers.IO).launch {
                    repository.toggleQuickSplit(txnId)
                    SpendWidgetProvider.refreshAllWidgets(context)
                }
                NotificationManagerCompat.from(context).cancel(txnId.toInt())
            }
        }
    }

    companion object {
        const val ACTION_CATEGORIZE = "com.aryaman.spendtracker.ACTION_CATEGORIZE"
        const val ACTION_SPLIT = "com.aryaman.spendtracker.ACTION_SPLIT"
        const val EXTRA_TXN_ID = "EXTRA_TXN_ID"
        const val EXTRA_CATEGORY = "EXTRA_CATEGORY"
        const val EXTRA_MERCHANT = "EXTRA_MERCHANT"
    }
}
