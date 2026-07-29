package com.teum.app.dashboard

import com.teum.app.data.local.entity.SessionLogEntity

enum class SessionHistorySortOption {
    LATEST,
    USAGE_ASCENDING,
    USAGE_DESCENDING,
    EXTENSION_ASCENDING,
    EXTENSION_DESCENDING
}

data class SessionHistoryPage(
    val sessions: List<SessionLogEntity>,
    val page: Int,
    val pageCount: Int
)

object SessionHistoryOrganizer {
    fun organize(
        sessions: List<SessionLogEntity>,
        selectedPackageName: String?,
        sortOption: SessionHistorySortOption,
        requestedPage: Int,
        pageSize: Int = 10
    ): SessionHistoryPage {
        require(pageSize > 0)

        val filtered = selectedPackageName?.let { selected ->
            sessions.filter { it.packageName == selected }
        } ?: sessions
        val sorted = when (sortOption) {
            SessionHistorySortOption.LATEST ->
                filtered.sortedByDescending { it.endedAtMillis }
            SessionHistorySortOption.USAGE_ASCENDING ->
                filtered.sortedWith(
                    compareBy<SessionLogEntity> {
                        SessionMetricsResolver.resolve(it).usageMillis
                    }.thenByDescending { it.endedAtMillis }
                )
            SessionHistorySortOption.USAGE_DESCENDING ->
                filtered.sortedWith(
                    compareByDescending<SessionLogEntity> {
                        SessionMetricsResolver.resolve(it).usageMillis
                    }.thenByDescending { it.endedAtMillis }
                )
            SessionHistorySortOption.EXTENSION_ASCENDING ->
                filtered.sortedWith(
                    compareBy<SessionLogEntity> { it.extensionCount }
                        .thenByDescending { it.endedAtMillis }
                )
            SessionHistorySortOption.EXTENSION_DESCENDING ->
                filtered.sortedWith(
                    compareByDescending<SessionLogEntity> { it.extensionCount }
                        .thenByDescending { it.endedAtMillis }
                )
        }
        val pageCount = maxOf(1, (sorted.size + pageSize - 1) / pageSize)
        val page = requestedPage.coerceIn(1, pageCount)
        val fromIndex = (page - 1) * pageSize
        val pageSessions = if (fromIndex >= sorted.size) {
            emptyList()
        } else {
            sorted.subList(fromIndex, minOf(fromIndex + pageSize, sorted.size))
        }

        return SessionHistoryPage(
            sessions = pageSessions,
            page = page,
            pageCount = pageCount
        )
    }
}
