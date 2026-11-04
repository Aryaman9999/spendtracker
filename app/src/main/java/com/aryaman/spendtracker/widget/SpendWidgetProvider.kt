package com.aryaman.spendtracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.aryaman.spendtracker.R
import com.aryaman.spendtracker.data.local.SpendDatabase
import com.aryaman.spendtracker.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        private const val TAG = "SpendWidget"
        const val ACTION_REFRESH = "com.aryaman.spendtracker.WIDGET_REFRESH"

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
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

                    val dao = SpendDatabase.getInstance(context).transactionDao()

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

                    // — Top 5 categories for current month —
                    val topCategories = dao.getTopCategoriesInWindowSync(monthStart, monthEnd, 5)

                    // Populate today and month amounts
                    views.setTextViewText(
                        R.id.widget_today_amount,
                        String.format(Locale.getDefault(), "₹%.0f", todaySpend)
                    )
                    views.setTextViewText(
                        R.id.widget_month_amount,
                        String.format(Locale.getDefault(), "₹%.0f", monthSpend)
                    )

                    // Category rows (up to 5)
                    val catRowIds = listOf(
                        Triple(R.id.widget_cat1_row, R.id.widget_cat1_name, R.id.widget_cat1_amount),
                        Triple(R.id.widget_cat2_row, R.id.widget_cat2_name, R.id.widget_cat2_amount),
                        Triple(R.id.widget_cat3_row, R.id.widget_cat3_name, R.id.widget_cat3_amount),
                        Triple(R.id.widget_cat4_row, R.id.widget_cat4_name, R.id.widget_cat4_amount),
                        Triple(R.id.widget_cat5_row, R.id.widget_cat5_name, R.id.widget_cat5_amount),
                    )

                    for (i in catRowIds.indices) {
                        val (rowId, nameId, amountId) = catRowIds[i]
                        if (i < topCategories.size) {
                            val cat = topCategories[i]
                            views.setTextViewText(nameId, cat.category.take(24))
                            views.setTextViewText(
                                amountId,
                                String.format(Locale.getDefault(), "₹%.0f", cat.totalAmount)
                            )
                            views.setViewVisibility(rowId, View.VISIBLE)
                        } else {
                            views.setViewVisibility(rowId, View.GONE)
                        }
                    }

                    // Empty categories state
                    views.setViewVisibility(
                        R.id.widget_no_cats,
                        if (topCategories.isEmpty()) View.VISIBLE else View.GONE
                    )

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update widget $appWidgetId", e)
                    try {
                        val fallback = RemoteViews(context.packageName, R.layout.widget_spend_tracker)
                        fallback.setTextViewText(R.id.widget_today_amount, "₹0")
                        fallback.setTextViewText(R.id.widget_month_amount, "₹0")
                        appWidgetManager.updateAppWidget(appWidgetId, fallback)
                    } catch (_: Exception) {}
                }
            }
        }

        fun refreshAllWidgets(context: Context) {
            val intent = Intent(context, SpendWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            context.sendBroadcast(intent)
        }
    }
}
