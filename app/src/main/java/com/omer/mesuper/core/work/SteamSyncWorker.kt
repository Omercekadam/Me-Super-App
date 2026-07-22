package com.omer.mesuper.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.omer.mesuper.feature.activity.data.ActivityRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Steam kütüphanesini günde bir kez senkronize eder (playtime_forever anlık görüntüsü). */
@HiltWorker
class SteamSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val activityRepository: ActivityRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        activityRepository.syncSteamLibrary()
        Result.success()
    } catch (e: IllegalArgumentException) {
        // Anahtarlar/Steam ID henüz Ayarlar'dan girilmemiş — tekrar denemenin anlamı yok, sessizce atla.
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}
