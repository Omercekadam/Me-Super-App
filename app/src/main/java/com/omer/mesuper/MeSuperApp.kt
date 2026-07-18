package com.omer.mesuper

import android.app.Application
import com.omer.mesuper.core.python.AnalyticsEngine
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MeSuperApp : Application() {

    @Inject
    lateinit var analyticsEngine: AnalyticsEngine

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Python yorumlayıcısının ilk açılışı ~1-2 sn sürer; kullanıcı beklemesin diye
        // uygulama açılır açılmaz arka planda ısıtıyoruz.
        appScope.launch {
            runCatching { analyticsEngine.warmUp() }
        }
    }
}
