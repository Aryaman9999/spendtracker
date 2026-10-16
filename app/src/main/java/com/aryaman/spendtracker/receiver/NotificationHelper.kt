package com.aryaman.spendtracker.receiver

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aryaman.spendtracker.R
import com.aryaman.spendtracker.SpendTrackerApp
import com.aryaman.spendtracker.ui.MainActivity
import java.util.Locale

object NotificationHelper {

    fun showReviewNotification(
        context: Context,
        transactionId: Long,
        amount: Double,
        merchant: String
    ) {
        val notificationId = transactionId.toInt()

        // Tap notification body to open app directly to the categorize dialog
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TRANSACTION_ID", transactionId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Helper to build quick category action pending intent
        fun createCategoryActionIntent(category: String, requestCodeOffset: Int): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_CATEGORIZE
                putExtra(NotificationActionReceiver.EXTRA_TXN_ID, transactionId)
                putExtra(NotificationActionReceiver.EXTRA_CATEGORY, category)
                putExtra(NotificationActionReceiver.EXTRA_MERCHANT, merchant)
            }
            return PendingIntent.getBroadcast(
                context,
                notificationId * 100 + requestCodeOffset,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val personalPendingIntent = createCategoryActionIntent("Personal Spend", 1)
        val foodPendingIntent = createCategoryActionIntent("Food & Dining", 2)
        val groceryPendingIntent = createCategoryActionIntent("Groceries", 3)
        val travelPendingIntent = createCategoryActionIntent("Travel & Commute", 4)

        val splitIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SPLIT
            putExtra(NotificationActionReceiver.EXTRA_TXN_ID, transactionId)
        }
        val splitPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 100 + 5,
            splitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = String.format(Locale.getDefault(), "₹%.2f", amount)

        // Build Custom Expanded View (Bypasses Android's 3-action limit, fitting all 4 categories without truncation!)
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_review_expanded).apply {
            setTextViewText(R.id.tv_notif_title, "Categorize Spend: $formattedAmount")
            setTextViewText(R.id.tv_notif_merchant, "Paid to $merchant. Tap a category or split:")

            setOnClickPendingIntent(R.id.btn_notif_personal, personalPendingIntent)
            setOnClickPendingIntent(R.id.btn_notif_food, foodPendingIntent)
            setOnClickPendingIntent(R.id.btn_notif_groceries, groceryPendingIntent)
            setOnClickPendingIntent(R.id.btn_notif_travel, travelPendingIntent)
            setOnClickPendingIntent(R.id.btn_notif_more, openAppPendingIntent)
        }

        val builder = NotificationCompat.Builder(context, SpendTrackerApp.CHANNEL_REVIEW_QUEUE)
            .setSmallIcon(R.drawable.ic_stat_spend)
            .setContentTitle("Categorize Spend: $formattedAmount")
            .setContentText("Paid to $merchant")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_SPEND_REVIEW)
            .setCustomBigContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            // Fallback compact actions for collapsed view (includes quick 50/50 split!)
            .addAction(0, "👤 Personal", personalPendingIntent)
            .addAction(0, "✂️ Split 50/50", splitPendingIntent)
            .addAction(0, "🍔 Food", foodPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission POST_NOTIFICATIONS may not be granted yet
        }
    }

    const val GROUP_SPEND_REVIEW = "com.aryaman.spendtracker.SPEND_REVIEW_GROUP"
}
