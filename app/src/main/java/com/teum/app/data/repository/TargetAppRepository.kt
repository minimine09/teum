package com.teum.app.data.repository

import android.content.Context

class TargetAppRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getTargetPackages(): Set<String> {
        return preferences.getStringSet(KEY_TARGET_PACKAGES, emptySet()).orEmpty()
            .filter { it.isNotBlank() }
            .toSet()
    }

    fun addTargetPackage(packageName: String) {
        val normalizedPackageName = packageName.trim()
        if (normalizedPackageName.isBlank()) return

        val nextPackages = getTargetPackages() + normalizedPackageName
        preferences.edit()
            .putStringSet(KEY_TARGET_PACKAGES, nextPackages)
            .apply()
    }

    fun removeTargetPackage(packageName: String) {
        val nextPackages = getTargetPackages() - packageName.trim()
        preferences.edit()
            .putStringSet(KEY_TARGET_PACKAGES, nextPackages)
            .apply()
    }

    fun isTargetPackage(packageName: String): Boolean {
        return getTargetPackages().contains(packageName.trim())
    }

    private companion object {
        const val PREFS_NAME = "teum_target_app_preferences"
        const val KEY_TARGET_PACKAGES = "target_packages"
    }
}
