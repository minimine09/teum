package com.teum.app.core.util

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.teum.app.accessibility.TeumAccessibilityService

object PermissionUtils {
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = ComponentName(context, TeumAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)

        return splitter.any { enabledService ->
            ComponentName.unflattenFromString(enabledService)?.let { componentName ->
                componentName == expectedComponentName
            } ?: false
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }
}
