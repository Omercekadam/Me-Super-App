package com.omer.mesuper.core.python

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kotlin ↔ Python köprüsü. Tüm analizler tek giriş noktasından geçer:
 * Python tarafındaki `analytics/api.py` içindeki `run(fn, payload_json)`.
 * Sözleşme: JSON girer, JSON çıkar. Python DB'ye asla dokunmaz.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class AnalyticsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // GIL nedeniyle Python çağrıları tek thread üzerinden akar.
    private val pyDispatcher = Dispatchers.IO.limitedParallelism(1)

    private val api by lazy {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        Python.getInstance().getModule("analytics.api")
    }

    suspend fun run(fn: String, payloadJson: String): String = withContext(pyDispatcher) {
        api.callAttr("run", fn, payloadJson).toString()
    }

    /** Yorumlayıcıyı ve pandas importunu önceden ısıtır; sonucu pandas sürümünü döner. */
    suspend fun warmUp(): String = run("ping", "{}")
}
