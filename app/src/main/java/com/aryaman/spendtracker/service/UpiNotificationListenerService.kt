package com.aryaman.spendtracker.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aryaman.spendtracker.SpendTrackerApp
import com.aryaman.spendtracker.data.parser.UpiNotificationParser
import com.aryaman.spendtracker.data.repository.TransactionProcessResult
import com.aryaman.spendtracker.receiver.NotificationHelper
import com.aryaman.spendtracker.widget.SpendWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpiNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (!UpiNotificationParser.isSupportedPackage(packageName)) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getString("android.title") ?: extras.getCharSequence("android.title")?.toString()
        val text = extras.getString("android.text") ?: extras.getCharSequence("android.text")?.toString()

        val parsed = UpiNotificationParser.parse(
            packageName = packageName,
            title = title,
            text = text,
            postTime = sbn.postTime
        ) ?: return

        val app = applicationContext as? SpendTrackerApp ?: return

        serviceScope.launch {
            val result = app.repository.processParsedTransaction(parsed)

            if (result is TransactionProcessResult.Inserted && result.isPendingReview) {
                NotificationHelper.showReviewNotification(
                    context = applicationContext,
                    transactionId = result.id,
                    amount = parsed.amount,
                    merchant = parsed.cleanMerchant
                )
            }

            // Refresh home screen widget after new transaction
            if (result is TransactionProcessResult.Inserted) {
                SpendWidgetProvider.refreshAllWidgets(applicationContext)
            }
        }
    }
}
