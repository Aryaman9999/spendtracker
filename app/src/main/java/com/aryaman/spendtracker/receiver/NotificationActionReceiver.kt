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
        val action = intent.action ?: return
        val txnId = intent.getLongExtra(EXTRA_TXN_ID, -1L)
        if (txnId == -1L) return

        val app = context.applicationContext as? SpendTrackerApp ?: return
        val repository = app.repository
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (action == ACTION_CATEGORIZE) {
                    val category = intent.getStringExtra(EXTRA_CATEGORY)
                    val merchant = intent.getStringExtra(EXTRA_MERCHANT)
                    if (!category.isNullOrBlank()) {
                        repository.setCategory(
                            transactionId = txnId,
                            category = category,
                            createRuleKeyword = merchant
                        )
                        SpendWidgetProvider.refreshAllWidgets(context)
                    }
                } else if (action == ACTION_SPLIT) {
                    repository.toggleQuickSplit(txnId)
                    SpendWidgetProvider.refreshAllWidgets(context)
                }
            } finally {
                NotificationHelper.dismissNotification(context, txnId)
                pendingResult.finish()
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
