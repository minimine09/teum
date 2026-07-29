package com.teum.app.dashboard

import com.teum.app.data.local.entity.SessionLogEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionHistoryOrganizerTest {
    @Test
    fun latestSortPlacesNewestSessionFirst() {
        val sessions = listOf(
            session(id = 1, packageName = "youtube", endedAt = 100),
            session(id = 2, packageName = "instagram", endedAt = 300),
            session(id = 3, packageName = "youtube", endedAt = 200)
        )

        val page = SessionHistoryOrganizer.organize(
            sessions = sessions,
            selectedPackageName = null,
            sortOption = SessionHistorySortOption.LATEST,
            requestedPage = 1
        )

        assertEquals(listOf(2L, 3L, 1L), page.sessions.map { it.id })
    }

    @Test
    fun appFilterIsAppliedBeforeSortingAndPaging() {
        val sessions = listOf(
            session(id = 1, packageName = "youtube", endedAt = 100),
            session(id = 2, packageName = "instagram", endedAt = 300),
            session(id = 3, packageName = "youtube", endedAt = 200)
        )

        val page = SessionHistoryOrganizer.organize(
            sessions = sessions,
            selectedPackageName = "youtube",
            sortOption = SessionHistorySortOption.LATEST,
            requestedPage = 1
        )

        assertEquals(listOf(3L, 1L), page.sessions.map { it.id })
    }

    @Test
    fun usageAndExtensionSortOptionsUseSessionMetrics() {
        val sessions = listOf(
            session(id = 1, packageName = "youtube", endedAt = 100, usageMillis = 30, extensions = 1),
            session(id = 2, packageName = "youtube", endedAt = 300, usageMillis = 10, extensions = 3),
            session(id = 3, packageName = "youtube", endedAt = 200, usageMillis = 20, extensions = 2)
        )

        val usageAscending = SessionHistoryOrganizer.organize(
            sessions,
            null,
            SessionHistorySortOption.USAGE_ASCENDING,
            1
        )
        val usageDescending = SessionHistoryOrganizer.organize(
            sessions,
            null,
            SessionHistorySortOption.USAGE_DESCENDING,
            1
        )
        val extensionAscending = SessionHistoryOrganizer.organize(
            sessions,
            null,
            SessionHistorySortOption.EXTENSION_ASCENDING,
            1
        )
        val extensionDescending = SessionHistoryOrganizer.organize(
            sessions,
            null,
            SessionHistorySortOption.EXTENSION_DESCENDING,
            1
        )

        assertEquals(listOf(2L, 3L, 1L), usageAscending.sessions.map { it.id })
        assertEquals(listOf(1L, 3L, 2L), usageDescending.sessions.map { it.id })
        assertEquals(listOf(1L, 3L, 2L), extensionAscending.sessions.map { it.id })
        assertEquals(listOf(2L, 3L, 1L), extensionDescending.sessions.map { it.id })
    }

    @Test
    fun paginationShowsTenItemsAndClampsInvalidPage() {
        val sessions = (1L..23L).map { id ->
            session(id = id, packageName = "youtube", endedAt = id)
        }

        val secondPage = SessionHistoryOrganizer.organize(
            sessions,
            null,
            SessionHistorySortOption.LATEST,
            requestedPage = 2
        )
        val tooLargePage = SessionHistoryOrganizer.organize(
            sessions,
            null,
            SessionHistorySortOption.LATEST,
            requestedPage = 99
        )

        assertEquals(3, secondPage.pageCount)
        assertEquals(2, secondPage.page)
        assertEquals((13L downTo 4L).toList(), secondPage.sessions.map { it.id })
        assertEquals(3, tooLargePage.page)
        assertEquals(listOf(3L, 2L, 1L), tooLargePage.sessions.map { it.id })
    }

    private fun session(
        id: Long,
        packageName: String,
        endedAt: Long,
        usageMillis: Long = 1L,
        extensions: Int = 0
    ) = SessionLogEntity(
        id = id,
        packageName = packageName,
        entryDetectedAtMillis = endedAt - usageMillis,
        startedAtMillis = endedAt - usageMillis,
        endedAtMillis = endedAt,
        durationMillis = usageMillis,
        targetDurationMillis = 1L,
        effectiveUsageMillis = usageMillis,
        finalTargetDurationMillis = 1L,
        intentChoice = "CLEAR_PURPOSE",
        outcomeType = null,
        overrun = false,
        extensionCount = extensions,
        isFastReopen = false,
        reopenGapMillis = null,
        createdAtMillis = endedAt
    )
}
