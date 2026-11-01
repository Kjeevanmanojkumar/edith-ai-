package com.edith.assistant.action

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import kotlin.math.abs

object AppLauncher {

    suspend fun findApp(context: Context, query: String): String? {
        val cleanQuery = query.lowercase().trim()
        if (cleanQuery.isEmpty()) return null

        val apps = InstalledAppLabelCache.get(context)

        // 1. Exact match
        apps[cleanQuery]?.let { return it }

        // 2. Candidate filtering
        val queryWords = cleanQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
        val candidates = mutableListOf<Pair<String, String>>() // label to pkg

        for ((label, pkg) in apps) {
            if (label.length < 3) continue

            val match = label.startsWith(cleanQuery) ||
                    cleanQuery.startsWith(label) ||
                    (queryWords.isNotEmpty() && queryWords.all { qWord ->
                        val labelWords = label.split(Regex("\\s+"))
                        labelWords.any { it.startsWith(qWord) }
                    })

            if (match) {
                candidates.add(label to pkg)
            }
        }

        if (candidates.isEmpty()) return null

        // Pick closest label length
        return candidates.minByOrNull { abs(it.first.length - cleanQuery.length) }?.second
    }

    fun launch(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
