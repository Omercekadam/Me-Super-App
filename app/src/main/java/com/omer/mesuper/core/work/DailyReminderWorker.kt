package com.omer.mesuper.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.omer.mesuper.core.notifications.NotificationHelper
import com.omer.mesuper.core.ui.formatKurusAsTl
import com.omer.mesuper.feature.agenda.data.AgendaRepository
import com.omer.mesuper.feature.finance.data.FinanceRepository
import com.omer.mesuper.feature.finance.data.SubscriptionEntity
import com.omer.mesuper.feature.finance.data.SubscriptionPeriod
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Günde bir kez: tiklenmemiş alışkanlıkları ve bugün vadesi gelen abonelikleri tek bir özet bildirimde toplar. */
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val agendaRepository: AgendaRepository,
    private val financeRepository: FinanceRepository,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val today = LocalDate.now()

        val habits = agendaRepository.habits.first()
        val tickedIds = agendaRepository.ticks.first().filter { it.date == today }.map { it.habitId }.toSet()
        val unticked = habits.filterNot { it.id in tickedIds }.map { it.name }

        val dueToday = financeRepository.subscriptions.first().filter { it.isActive && isDueToday(it, today) }

        val lines = buildList {
            if (unticked.isNotEmpty()) add("🔗 Bugün tiklenmeyen: ${unticked.joinToString(", ")}")
            dueToday.forEach { add("💳 Bugün ödeme günü: ${it.name} (${it.amountKurus.formatKurusAsTl()})") }
        }
        if (lines.isNotEmpty()) notificationHelper.notifyDailyDigest(lines.joinToString("\n"))

        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    private fun isDueToday(sub: SubscriptionEntity, today: LocalDate): Boolean = when (sub.period) {
        SubscriptionPeriod.MONTHLY -> sub.billingDay == today.dayOfMonth
        SubscriptionPeriod.YEARLY -> sub.billingDay == today.dayOfMonth && sub.billingMonth == today.monthValue
    }
}
