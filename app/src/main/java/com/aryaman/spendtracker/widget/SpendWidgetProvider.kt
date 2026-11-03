package com.aryaman.spendtracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.aryaman.spendtracker.R
import com.aryaman.spendtracker.data.local.SpendDatabase
import com.aryaman.spendtracker.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class SpendWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, SpendWidgetProvider::class.java)
            )
            for (id in widgetIds) {
                updateWidget(context, appWidgetManager, id)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.aryaman.spendtracker.WIDGET_REFRESH"

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_spend_tracker)

            // Tap widget body → open app
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // Refresh button
            val refreshIntent = Intent(context, SpendWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 1, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

            // Show loading state
            views.setTextViewText(R.id.widget_today_amount, "...")
            views.setTextViewText(R.id.widget_month_amount, "...")
            appWidgetManager.updateAppWidget(appWidgetId, views)

            // Fetch data asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = SpendDatabase.getInstance(context).transactionDao()
                    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

                    // — Today's spend —
                    val todayCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val todayStart = todayCal.timeInMillis
                    val todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1
                    val todaySpend = dao.getTotalSpendInWindowSync(todayStart, todayEnd) ?: 0.0

                    // — This month's spend —
                    val monthCal = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val monthStart = monthCal.timeInMillis
                    val monthEnd = System.currentTimeMillis()
                    val monthSpend = dao.getTotalSpendInWindowSync(monthStart, monthEnd) ?: 0.0

                    // — Recent 3 debits —
                    val recentTxns = dao.getRecentDebitsSync(3)

                    // Populate amounts
                    views.setTextViewText(R.id.widget_today_amount, formatter.format(todaySpend))
                    views.setTextViewText(R.id.widget_month_amount, formatter.format(monthSpend))

                    // Transaction rows
                    val rowIds = listOf(
                        Triple(R.id.widget_txn1_row, R.id.widget_txn1_desc, R.id.widget_txn1_amount),
                        Triple(R.id.widget_txn2_row, R.id.widget_txn2_desc, R.id.widget_txn2_amount),
                        Triple(R.id.widget_txn3_row, R.id.widget_txn3_desc, R.id.widget_txn3_amount),
                    )

                    for (i in rowIds.indices) {
                        val (rowId, descId, amountId) = rowIds[i]
                        if (i < recentTxns.size) {
                            val txn = recentTxns[i]
                            val label = txn.cleanMerchant
                                ?: txn.merchantText?.take(20)
                                ?: txn.category
                                ?: "Transaction"
                            views.setTextViewText(descId, label.take(20))
                            views.setTextViewText(amountId, formatter.format(txn.myActualShare))
                            views.setViewVisibility(rowId, View.VISIBLE)
                        } else {
                            views.setViewVisibility(rowId, View.GONE)
                        }
                    }

                    // No transactions message
                    views.setViewVisibility(
                        R.id.widget_no_txns,
                        if (recentTxns.isEmpty()) View.VISIBLE else View.GONE
                    )

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    views.setTextViewText(R.id.widget_today_amount, "₹0")
                    views.setTextViewText(R.id.widget_month_amount, "₹0")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        /**
         * Call this from SmsReceiver / UpiNotificationListenerService after a
         * new transaction is inserted to immediately refresh all widgets.
         */
        fun refreshAllWidgets(context: Context) {
            val intent = Intent(context, SpendWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            context.sendBroadcast(intent)
        }
    }
}
