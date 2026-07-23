package com.omer.mesuper.core.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object DailyReminderScheduler {
    private const val WORK_NAME = "daily_reminder"
    private const val TARGET_HOUR = 20 // akşam 20:00 özet hatırlatması

    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(TARGET_HOUR, 0)
        if (!now.isBefore(target)) target = target.plusDays(1)
        val initialDelay = Duration.between(now, target).toMillis()

        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
