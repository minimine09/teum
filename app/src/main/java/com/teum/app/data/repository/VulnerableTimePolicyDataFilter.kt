package com.teum.app.data.repository

import com.teum.app.data.local.entity.AppOpenEventEntity
import com.teum.app.data.local.entity.SessionLogEntity

internal object VulnerableTimePolicyDataFilter {
    fun sessions(
        sessions: List<SessionLogEntity>,
        targetPackages: Set<String>
    ): List<SessionLogEntity> {
        return sessions.filter { session -> session.packageName in targetPackages }
    }

    fun openEvents(
        openEvents: List<AppOpenEventEntity>,
        targetPackages: Set<String>
    ): List<AppOpenEventEntity> {
        return openEvents.filter { event -> event.packageName in targetPackages }
    }
}
