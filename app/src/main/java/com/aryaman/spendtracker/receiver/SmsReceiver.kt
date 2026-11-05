package com.aryaman.spendtracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.aryaman.spendtracker.SpendTrackerApp
import com.aryaman.spendtracker.data.parser.DynamicTemplateParser
import com.aryaman.spendtracker.data.repository.TransactionProcessResult
import com.aryaman.spendtracker.widget.SpendWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].displayOriginatingAddress ?: ""
        val fullBody = messages.joinToString("") { it.displayMessageBody ?: "" }
        val timestamp = messages[0].timestampMillis

        val app = context.applicationContext as? SpendTrackerApp ?: return
        val repository = app.repository

        // GoAsync to allow asynchronous Room DB writing in BroadcastReceiver
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Dynamic bank format templates (both preloaded and user-configured)
                val activeTemplates = repository.getActiveTemplatesList()
                val parsed = DynamicTemplateParser.parse(fullBody, sender, timestamp, activeTemplates)

                if (parsed == null) return@launch

                val result = repository.processParsedTransaction(parsed)

                if (result is TransactionProcessResult.Inserted && result.isPendingReview) {
                    NotificationHelper.showReviewNotification(
                        context = context,
                        transactionId = result.id,
                        amount = parsed.amount,
                        merchant = parsed.cleanMerchant
                    )
                }

                // Refresh home screen widget after new transaction
                if (result is TransactionProcessResult.Inserted) {
                    SpendWidgetProvider.refreshAllWidgets(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
