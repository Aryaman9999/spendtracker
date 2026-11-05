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
import com.aryaman.spendtracker.data.local.entity.CategoryEntity
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
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, SpendWidgetProvider::class.java)
            )
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    for (id in widgetIds) {
                        updateWidget(context, appWidgetManager, id)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private data class WidgetBarItem(
        val categoryName: String,
        val displayName: String,
        val amountText: String,
        val progressPercent: Int,
        val isOverBudget: Boolean
    )

    private data class WidgetRowViews(
        val rowId: Int,
        val nameId: Int,
        val amountId: Int,
        val progressId: Int,
        val progressAlertId: Int
    )

    companion object {
        private const val TAG = "SpendWidget"
        const val ACTION_REFRESH = "com.aryaman.spendtracker.WIDGET_REFRESH"

        private fun getCategoryEmoji(category: String?): String {
            return when (category?.lowercase(Locale.ROOT)?.trim()) {
                "food & dining", "food", "dining" -> "🍔"
                "groceries", "grocery" -> "🛒"
                "travel & commute", "travel", "commute" -> "✈️"
                "personal spend", "personal" -> "👤"
                "shopping" -> "🛍️"
                "bills & utilities", "bills", "utilities" -> "⚡"
                "entertainment", "movies" -> "🎬"
                "health & medical", "health", "medical" -> "💊"
                "rent" -> "🏠"
                "subscriptions", "subscription" -> "🔁"
                "emi & loans", "emi", "loans", "loan" -> "🏦"
                "investments", "investment" -> "📈"
                "education" -> "📚"
                "fuel", "petrol" -> "⛽"
                "insurance" -> "🛡️"
                "gifts & donations", "gifts", "gift", "donations" -> "🎁"
                else -> "💳"
            }
        }

        private suspend fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
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

                    val db = SpendDatabase.getInstance(context)
                    val txnDao = db.transactionDao()
                    val catDao = db.categoryDao()

                    // — Today's spend —
                    val todayCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val todayStart = todayCal.timeInMillis
                    val todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1
                    val todaySpend = txnDao.getTotalSpendInWindowSync(todayStart, todayEnd) ?: 0.0

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
                    val monthSpend = txnDao.getTotalSpendInWindowSync(monthStart, monthEnd) ?: 0.0

                    // Populate today and month amounts
                    views.setTextViewText(
                        R.id.widget_today_amount,
                        String.format(Locale.getDefault(), "₹%,.0f", todaySpend)
                    )
                    views.setTextViewText(
                        R.id.widget_month_amount,
                        String.format(Locale.getDefault(), "₹%,.0f", monthSpend)
                    )

                    // — Category Budgets & Spending Data —
                    val allCategories = catDao.getAllCategoriesList()
                    val budgetSpendList = txnDao.getBudgetSpendByCategorySync(monthStart, monthEnd)
                    val budgetSpendMap = budgetSpendList.associate { it.category to it.totalAmount }

                    val totalSpendList = txnDao.getTopCategoriesInWindowSync(monthStart, monthEnd, 50)
                    val totalSpendMap = totalSpendList.associate { it.category to it.totalAmount }

                    val budgetedCategories = allCategories.filter { it.monthlyBudget != null && it.monthlyBudget!! > 0 }

                    val widgetItems = mutableListOf<WidgetBarItem>()

                    // 1. Process categories with a configured budget
                    val budgetedItems = budgetedCategories.map { cat ->
                        val budget = cat.monthlyBudget!!
                        val budgetedSpend = budgetSpendMap[cat.name] ?: 0.0
                        val ratio = if (budget > 0) budgetedSpend / budget else 0.0
                        val percent = (ratio * 100).toInt()
                        val isOver = budgetedSpend > budget
                        val emoji = getCategoryEmoji(cat.name)
                        val displayTitle = "$emoji ${cat.name}"

                        val amountStr = if (isOver) {
                            String.format(Locale.getDefault(), "₹%,.0f / ₹%,.0f (!)", budgetedSpend, budget)
                        } else {
                            String.format(Locale.getDefault(), "₹%,.0f / ₹%,.0f (%d%%)", budgetedSpend, budget, percent)
                        }

                        Triple(
                            cat,
                            WidgetBarItem(
                                categoryName = cat.name,
                                displayName = displayTitle,
                                amountText = amountStr,
                                progressPercent = percent.coerceIn(0, 100),
                                isOverBudget = isOver
                            ),
                            ratio
                        )
                    }
                    .sortedWith(
                        compareByDescending<Triple<CategoryEntity, WidgetBarItem, Double>> { it.second.isOverBudget }
                            .thenByDescending { it.third }
                    )
                    .map { it.second }

                    widgetItems.addAll(budgetedItems)

                    // 2. If fewer than 4 budgeted categories, fill remaining with highest spending categories
                    if (widgetItems.size < 4) {
                        val existingNames = widgetItems.map { it.categoryName.lowercase(Locale.ROOT) }.toSet()
                        val nonBudgetedTop = totalSpendMap.entries
                            .filter { it.key.lowercase(Locale.ROOT) !in existingNames }
                            .sortedByDescending { it.value }
                            .take(4 - widgetItems.size)

                        for ((catName, spend) in nonBudgetedTop) {
                            val emoji = getCategoryEmoji(catName)
                            val percent = if (monthSpend > 0) ((spend / monthSpend) * 100).toInt() else 0
                            val amountStr = String.format(Locale.getDefault(), "₹%,.0f (%d%%)", spend, percent)
                            widgetItems.add(
                                WidgetBarItem(
                                    categoryName = catName,
                                    displayName = "$emoji $catName",
                                    amountText = amountStr,
                                    progressPercent = percent.coerceIn(0, 100),
                                    isOverBudget = false
                                )
                            )
                        }
                    }

                    val finalItems = widgetItems.take(4)

                    // 4 Category Row View references
                    val rows = listOf(
                        WidgetRowViews(R.id.widget_cat1_row, R.id.widget_cat1_name, R.id.widget_cat1_amount, R.id.widget_cat1_progress, R.id.widget_cat1_progress_alert),
                        WidgetRowViews(R.id.widget_cat2_row, R.id.widget_cat2_name, R.id.widget_cat2_amount, R.id.widget_cat2_progress, R.id.widget_cat2_progress_alert),
                        WidgetRowViews(R.id.widget_cat3_row, R.id.widget_cat3_name, R.id.widget_cat3_amount, R.id.widget_cat3_progress, R.id.widget_cat3_progress_alert),
                        WidgetRowViews(R.id.widget_cat4_row, R.id.widget_cat4_name, R.id.widget_cat4_amount, R.id.widget_cat4_progress, R.id.widget_cat4_progress_alert),
                    )

                    for (i in rows.indices) {
                        val r = rows[i]
                        if (i < finalItems.size) {
                            val item = finalItems[i]

                            views.setTextViewText(r.nameId, item.displayName)
                            views.setTextViewText(r.amountId, item.amountText)

                            if (item.isOverBudget) {
                                views.setTextColor(r.amountId, 0xFFFF6B6B.toInt()) // Soft coral red
                                views.setViewVisibility(r.progressId, View.GONE)
                                views.setViewVisibility(r.progressAlertId, View.VISIBLE)
                                views.setProgressBar(r.progressAlertId, 100, 100, false)
                            } else {
                                views.setTextColor(r.amountId, 0xFFFFFFFF.toInt()) // Crisp white
                                views.setViewVisibility(r.progressId, View.VISIBLE)
                                views.setViewVisibility(r.progressAlertId, View.GONE)
                                views.setProgressBar(r.progressId, 100, item.progressPercent, false)
                            }

                            views.setViewVisibility(r.rowId, View.VISIBLE)
                        } else {
                            views.setViewVisibility(r.rowId, View.GONE)
                        }
                    }

                    // Empty state
                    views.setViewVisibility(
                        R.id.widget_no_cats,
                        if (finalItems.isEmpty()) View.VISIBLE else View.GONE
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

        fun refreshAllWidgets(context: Context) {
            val intent = Intent(context, SpendWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
                `package` = context.packageName
            }
            context.sendBroadcast(intent)
        }
    }
}
