package com.teum.app.dashboard

import android.content.Context
import android.content.pm.PackageManager

class AppDisplayNameResolver(context: Context) {
    private val packageManager = context.applicationContext.packageManager
    private val cache = mutableMapOf<String, String>()

    fun resolve(packageName: String): String = cache.getOrPut(packageName) {
        try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
                .takeIf { it.isNotBlank() }
                ?: packageName
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
