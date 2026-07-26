package com.teum.app.data.repository

import android.content.Context
import com.teum.app.core.model.InterventionMode

class UserSettingsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getInterventionMode(): InterventionMode {
        val savedValue = preferences.getString(KEY_INTERVENTION_MODE, null)
        return InterventionMode.entries.firstOrNull { it.name == savedValue }
            ?: InterventionMode.NORMAL
    }

    fun setInterventionMode(mode: InterventionMode) {
        preferences.edit()
            .putString(KEY_INTERVENTION_MODE, mode.name)
            .apply()
    }

    fun isSetupCompleted(): Boolean {
        return preferences.getBoolean(KEY_SETUP_COMPLETED, false)
    }

    fun setSetupCompleted(completed: Boolean) {
        preferences.edit()
            .putBoolean(KEY_SETUP_COMPLETED, completed)
            .apply()
    }

    fun getForceVulnerableNowForDebug(): Boolean {
        return preferences.getBoolean(KEY_FORCE_VULNERABLE_NOW_FOR_DEBUG, false)
    }

    fun setForceVulnerableNowForDebug(value: Boolean) {
        preferences.edit()
            .putBoolean(KEY_FORCE_VULNERABLE_NOW_FOR_DEBUG, value)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "teum_user_settings"
        const val KEY_INTERVENTION_MODE = "teum_mode"
        const val KEY_SETUP_COMPLETED = "setup_completed"
        const val KEY_FORCE_VULNERABLE_NOW_FOR_DEBUG = "force_vulnerable_now_for_debug"
    }
}
