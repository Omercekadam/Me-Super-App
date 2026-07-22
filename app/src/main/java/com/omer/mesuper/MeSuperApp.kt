package com.omer.mesuper

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.omer.mesuper.core.python.AnalyticsEngine
import com.omer.mesuper.core.work.SteamSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MeSuperApp : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var analyticsEngine: AnalyticsEngine

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .build()

    override fun onCreate() {
        super.onCreate()
        // WorkManager'ın androidx.startup üzerinden otomatik başlaması Application.onCreate()'ten
        // önce olur; o an workerFactory henüz Hilt tarafından enjekte edilmemiştir. Bu yüzden
        // manifest'te devre dışı bırakıp burada, enjeksiyon tamamlandıktan sonra elle başlatıyoruz.
        WorkManager.initialize(this, Configuration.Builder().setWorkerFactory(workerFactory).build())

        // Python yorumlayıcısının ilk açılışı ~1-2 sn sürer; kullanıcı beklemesin diye
        // uygulama açılır açılmaz arka planda ısıtıyoruz.
        appScope.launch {
            runCatching { analyticsEngine.warmUp() }
        }
        SteamSyncScheduler.schedule(this)
    }
}
