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

    private companion object {
        const val PREFS_NAME = "teum_user_settings"
        const val KEY_INTERVENTION_MODE = "teum_mode"
    }
}
