package com.edith.assistant.action

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object InstalledAppLabelCache {
    private const val TTL_MS = 15 * 60 * 1000L // 15 minutes
    private var cacheTimestamp = 0L
    private val appMap = mutableMapOf<String, String>()
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun prewarm(context: Context) {
        scope.launch {
            get(context)
        }
    }

    suspend fun get(context: Context): Map<String, String> {
        return mutex.withLock {
            val now = System.currentTimeMillis()
            if (appMap.isNotEmpty() && (now - cacheTimestamp < TTL_MS)) {
                return@withLock appMap.toMap()
            }

            appMap.clear()
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            for (ri in resolveInfos) {
                val label = ri.loadLabel(pm)?.toString()?.trim() ?: continue
                val pkg = ri.activityInfo?.packageName ?: continue
                if (label.isNotEmpty()) {
                    appMap[label.lowercase()] = pkg
                }
            }
            cacheTimestamp = now
            appMap.toMap()
        }
    }
}
