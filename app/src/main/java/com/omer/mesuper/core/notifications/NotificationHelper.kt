package com.omer.mesuper.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.omer.mesuper.MainActivity
import com.omer.mesuper.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_BUDGET = "budget_alerts"
        const val CHANNEL_DAILY = "daily_reminders"
        const val DAILY_DIGEST_ID = 9000
        private const val BUDGET_ID_OFFSET = 10000
    }

    init {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_BUDGET, "Bütçe Uyarıları", NotificationManager.IMPORTANCE_DEFAULT)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DAILY, "Günlük Hatırlatmalar", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun notifyBudgetExceeded(budgetId: Long, title: String, text: String) {
        if (!hasPermission()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(BUDGET_ID_OFFSET + budgetId.toInt(), notification)
    }

    fun notifyDailyDigest(text: String) {
        if (!hasPermission()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Me SuperApp")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(DAILY_DIGEST_ID, notification)
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
