package com.teum.app.demo

import com.teum.app.core.model.InterventionMode
import com.teum.app.data.local.entity.AppOpenEventEntity
import com.teum.app.data.local.entity.ExtensionEventEntity
import com.teum.app.data.local.entity.SelfControlEventEntity
import com.teum.app.data.local.entity.SessionLogEntity
import com.teum.app.data.repository.SessionLogRepository
import com.teum.app.overlay.IntentChoice
import com.teum.app.session.OutcomeType
import java.util.Calendar
import java.util.TimeZone

internal data class DemoDataset(
    val sessions: List<DemoSessionSeed>,
    val reopenLogs: List<DemoReopenSeed>,
    val appOpenEvents: List<AppOpenEventEntity>,
    val selfControlEvents: List<SelfControlEventEntity>
)

internal data class DemoSessionSeed(
    val key: String,
    val entity: SessionLogEntity,
    val extensionEvents: List<DemoExtensionSeed> = emptyList()
)

internal data class DemoExtensionSeed(
    val occurredAtMillis: Long,
    val extensionDurationMillis: Long,
    val interventionActiveAtTime: Boolean
)

internal data class DemoReopenSeed(
    val previousKey: String,
    val currentKey: String,
    val gapTimeMillis: Long,
    val isFastReopen: Boolean
)

internal object DemoDatasetFactory {
    fun canCreateTodayData(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        return todaySchedule(nowMillis).earliestTimestampMillis >= startOfTodayMillis(
            nowMillis = nowMillis,
            timeZone = timeZone
        )
    }

    fun minimumRequiredTodayElapsedMillis(): Long =
        nowMillisFromTodayScheduleOffset()

    fun safeStartWarning(): String =
        "오늘 데이터 생성에 필요한 시간이 아직 부족합니다."

    fun create(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): DemoDataset {
        require(canCreateTodayData(nowMillis, timeZone)) { safeStartWarning() }

        val day = DemoDayClock(nowMillis, timeZone)
        val todaySchedule = todaySchedule(nowMillis)
        require(
            todaySchedule.earliestTimestampMillis >= startOfTodayMillis(
                nowMillis = nowMillis,
                timeZone = timeZone
            )
        ) {
            safeStartWarning()
        }

        val s15 = session(
            key = "S15",
            entryAt = todaySchedule.s15EntryMillis,
            packageName = YOUTUBE,
            appDisplayName = DemoToolsContract.YOUTUBE_NAME,
            intent = IntentChoice.CLEAR_PURPOSE,
            targetMinutes = 5,
            usageMinutes = 4,
            outcome = null
        )
        val s16Entry = todaySchedule.s16EntryMillis

        val sessions = listOf(
            session(
                key = "S01",
                entryAt = day.at(-6, 10, 0),
                packageName = YOUTUBE,
                appDisplayName = DemoToolsContract.YOUTUBE_NAME,
                intent = IntentChoice.CLEAR_PURPOSE,
                targetMinutes = 5,
                usageMinutes = 4,
                outcome = OutcomeType.PURPOSE_ACHIEVED
            ),
            session(
                key = "S02",
                entryAt = day.at(-6, 22, 5),
                packageName = INSTAGRAM,
                appDisplayName = DemoToolsContract.INSTAGRAM_NAME,
                intent = IntentChoice.CLEAR_PURPOSE,
                targetMinutes = 5,
                usageMinutes = 10,
                outcome = OutcomeType.PURPOSE_DRIFT,
                extensionCount = 1,
                interventionAppliedAtStart = true,
                vulnerableAtStart = true,
                closedAfterIntervention = true,
                cautionExtensionCount = 1
            ),
            session(
                key = "S03",
                entryAt = day.at(-5, 12, 20),
                packageName = CHROME,
                appDisplayName = DemoToolsContract.CHROME_NAME,
                intent = IntentChoice.CLEAR_PURPOSE,
                targetMinutes = 3,
                usageMinutes = 5,
                outcome = OutcomeType.NECESSARY_USE
            ),
            session(
                key = "S04",
                entryAt = day.at(-5, 21, 56),
                packageName = YOUTUBE,
                appDisplayName = DemoToolsContract.YOUTUBE_NAME,
                intent = IntentChoice.MINDFUL_REST,
                targetMinutes = 3,
                usageMinutes = 2,
                outcome = null
            )
        )
        val s04 = sessions.last()
        val s05Entry = s04.entity.endedAtMillis + THREE_MINUTES

        val remainingSessions = listOf(
            session(
                key = "S05",
                entryAt = s05Entry,
                packageName = YOUTUBE,
                appDisplayName = DemoToolsContract.YOUTUBE_NAME,
                intent = IntentChoice.CLEAR_PURPOSE,
                targetMinutes = 5,
                usageMinutes = 7,
                outcome = OutcomeType.PURPOSE_DRIFT,
                interventionAppliedAtStart = true,
                vulnerableAtStart = true,
                closedAfterIntervention = true
            ),
            session(
                key = "S06",
                entryAt = day.at(-4, 14, 0),
                packageName = INSTAGRAM,
                appDisplayName = DemoToolsContract.INSTAGRAM_NAME,
                intent = IntentChoice.CLEAR_PURPOSE,
                targetMinutes = 5,
                usageMinutes = 4,
                outcome = OutcomeType.PURPOSE_ACHIEVED
            ),
            session(
                key = "S07",
                entryAt = day.at(-4, 21, 58),
                packageName = INSTAGRAM,
                appDisplayName = DemoToolsContract.INSTAGRAM_NAME,
                intent = IntentChoice.MINDFUL_REST,
                targetMinutes = 3,
                usageMinutes = 7,
                outcome = OutcomeType.EXTENDED,
                extensionCount = 1,
                interventionEverApplied = true,
                cautionExtensionCount = 1
            ),
            session(
                key = "S08",
                entryAt = day.at(-3, 11, 15),
                packageName = CHROME,
                appDisplayName = DemoToolsContract.CHROME_NAME,
                intent = IntentChoice.CLEAR_PURPOSE,
                targetMinutes = 5,
                usageMinutes = 6,
                outcome = OutcomeType.NECESSARY_USE
            ),
            session(
                key = "S09",
                entryAt = day.at(-3, 21, 54),
                packageName = YOUTUBE,
                appDisplayName = DemoToolsContract.YOUTUBE_NAME,
                intent = IntentChoice.UNCONSCIOUS_OPEN,
                targetMinutes = 3,
                usageMinutes = 2,
                outcome = null
            )
        )
        val s09 = remainingSessions.last()
        val s10Entry = s09.entity.endedAtMillis + FIVE_MINUTES

        val tailSessions = listOf(
            session(
                key = "S10",
                entryAt = s10Entry,
                packageName = YOUTUBE,
                appDisplayName = DemoToolsContract.YOUTUBE_NAME,
                intent = IntentChoice.CLEAR_PURPOSE,
                targetMinutes = 5,
                usageMinutes = 12,
                outcome = OutcomeType.PURPOSE_DRIFT,
                extensionCount = 2,
                interventionAppliedAtStart = true,
                vulnerableAtStart = true,
                closedAfterIntervention = true,
                cautionExtensionCount = 2
            ),
            session(
                key = "S11",
                entryAt = day.at(-2, 16, 20),
                packageName = INSTAGRAM,
                appDisplayName = DemoToolsContract.INSTAGRAM_NAME,
                intent = IntentChoice.CLEAR_PURPOSE,
                targetMinutes = 5,
                usageMinutes = 5,
                outcome = OutcomeType.PURPOSE_ACHIEVED
            ),
            session(
                key = "S12",
                entryAt = day.at(-2, 22, 20),
                packageName = YOUTUBE,
                appDisplayName = DemoToolsContract.YOUTUBE_NAME,
                intent = IntentChoice.CLEAR_PURPOSE,
                targetMinutes = 5,
                usageMinutes = 8,
                outcome = OutcomeType.CONTINUED_SCROLLING,
                interventionAppliedAtStart = true,
                vulnerableAtStart = true
            ),
            session(
                key = "S13",
                entryAt = day.at(-1, 8, 30),
                packageName = CHROME,
                appDisplayName = DemoToolsContract.CHROME_NAME,
                intent = IntentChoice.CLEAR_PURPOSE,
                targetMinutes = 3,
                usageMinutes = 2,
                outcome = OutcomeType.PURPOSE_ACHIEVED
            ),
            session(
                key = "S14",
                entryAt = day.at(-1, 22, 8),
                packageName = INSTAGRAM,
                appDisplayName = DemoToolsContract.INSTAGRAM_NAME,
                intent = IntentChoice.CLEAR_PURPOSE,
                targetMinutes = 5,
                usageMinutes = 9,
                outcome = OutcomeType.NECESSARY_USE,
                extensionCount = 1,
                interventionAppliedAtStart = true,
                vulnerableAtStart = true,
                cautionExtensionCount = 1
            ),
            s15,
            session(
                key = "S16",
                entryAt = s16Entry,
                packageName = YOUTUBE,
                appDisplayName = DemoToolsContract.YOUTUBE_NAME,
                intent = IntentChoice.MINDFUL_REST,
                targetMinutes = 3,
                usageMinutes = 5,
                outcome = OutcomeType.EXTENDED,
                extensionCount = 1
            ),
            session(
                key = "S17",
                entryAt = todaySchedule.s17EntryMillis,
                packageName = INSTAGRAM,
                appDisplayName = DemoToolsContract.INSTAGRAM_NAME,
                intent = IntentChoice.CLEAR_PURPOSE,
                targetMinutes = 5,
                usageMinutes = 6,
                outcome = OutcomeType.PURPOSE_DRIFT
            )
        )
        val allSessions = sessions + remainingSessions + tailSessions
        require(allSessions.all { it.entity.endedAtMillis < nowMillis }) {
            safeStartWarning()
        }

        return DemoDataset(
            sessions = allSessions,
            reopenLogs = listOf(
                DemoReopenSeed("S04", "S05", THREE_MINUTES, true),
                DemoReopenSeed("S09", "S10", FIVE_MINUTES, true),
                DemoReopenSeed("S15", "S16", THREE_MINUTES, true)
            ),
            appOpenEvents = allSessions.map {
                AppOpenEventEntity(
                    packageName = it.entity.packageName,
                    detectedAtMillis = it.entity.entryDetectedAtMillis
                )
            } + listOf(
                AppOpenEventEntity(
                    packageName = YOUTUBE,
                    detectedAtMillis = day.at(-4, 22, 50)
                ),
                AppOpenEventEntity(
                    packageName = INSTAGRAM,
                    detectedAtMillis = day.at(-2, 22, 50)
                ),
                AppOpenEventEntity(
                    packageName = CHROME,
                    detectedAtMillis = todaySchedule.closeNowMillis
                )
            ),
            selfControlEvents = listOf(
                selfControl(YOUTUBE, DemoToolsContract.YOUTUBE_NAME, day.at(-4, 22, 50), true),
                selfControl(INSTAGRAM, DemoToolsContract.INSTAGRAM_NAME, day.at(-2, 22, 50), true),
                selfControl(CHROME, DemoToolsContract.CHROME_NAME, todaySchedule.closeNowMillis, false)
            )
        )
    }

    private fun session(
        key: String,
        entryAt: Long,
        packageName: String,
        appDisplayName: String,
        intent: IntentChoice,
        targetMinutes: Int,
        usageMinutes: Int,
        outcome: OutcomeType?,
        extensionCount: Int = 0,
        interventionAppliedAtStart: Boolean = false,
        vulnerableAtStart: Boolean = false,
        interventionEverApplied: Boolean = interventionAppliedAtStart,
        closedAfterIntervention: Boolean = false,
        cautionExtensionCount: Int = 0
    ): DemoSessionSeed {
        val startedAt = entryAt + START_DELAY_MILLIS
        val targetMillis = targetMinutes * MILLIS_PER_MINUTE
        val usageMillis = usageMinutes * MILLIS_PER_MINUTE
        val extensionDurationMillis = extensionCount * THREE_MINUTES
        val interventionVisibleMillis = interventionVisibleMillis(
            extensionCount = extensionCount,
            closedAfterIntervention = closedAfterIntervention,
            interventionEverApplied = interventionEverApplied
        )
        val durationMillis = usageMillis + interventionVisibleMillis
        val endedAt = startedAt + durationMillis
        val finalTargetMillis = targetMillis + extensionDurationMillis
        val rawOverrunMillis = (usageMillis - finalTargetMillis).coerceAtLeast(0L)
        val outcomeRespondedAtMillis = outcome?.let { endedAt + OUTCOME_RESPONSE_DELAY_MILLIS }
        val extensionEvents = (0 until extensionCount).map { index ->
            DemoExtensionSeed(
                occurredAtMillis = startedAt + targetMillis + index * THREE_MINUTES,
                extensionDurationMillis = THREE_MINUTES,
                interventionActiveAtTime = interventionAppliedAtStart || interventionEverApplied
            )
        }

        return DemoSessionSeed(
            key = key,
            entity = SessionLogEntity(
                packageName = packageName,
                appDisplayName = appDisplayName,
                entryDetectedAtMillis = entryAt,
                startedAtMillis = startedAt,
                endedAtMillis = endedAt,
                durationMillis = durationMillis,
                targetDurationMillis = targetMillis,
                interventionVisibleMillis = interventionVisibleMillis,
                effectiveUsageMillis = usageMillis,
                totalExtensionDurationMillis = extensionDurationMillis,
                finalTargetDurationMillis = finalTargetMillis,
                rawOverrunMillis = rawOverrunMillis,
                overrunDetectedAtMillis = rawOverrunMillis
                    .takeIf { it > 0L }
                    ?.let { endedAt - it },
                overrunMillis = rawOverrunMillis,
                necessaryUseExcessMillis = if (
                    intent == IntentChoice.CLEAR_PURPOSE &&
                    outcome == OutcomeType.NECESSARY_USE
                ) {
                    rawOverrunMillis
                } else {
                    0L
                },
                intentChoice = intent.name,
                modeAtStart = InterventionMode.INTERVENTION.name,
                isVulnerableTimeAtStart = vulnerableAtStart,
                interventionAppliedAtStart = interventionAppliedAtStart,
                outcomeType = outcome?.name,
                outcomeRespondedAtMillis = outcomeRespondedAtMillis,
                outcomeAchieved = when (outcome) {
                    OutcomeType.PURPOSE_ACHIEVED -> true
                    OutcomeType.NECESSARY_USE,
                    OutcomeType.PURPOSE_DRIFT,
                    OutcomeType.CONTINUED_SCROLLING -> false
                    else -> null
                },
                purposeDrifted = when (outcome) {
                    OutcomeType.PURPOSE_ACHIEVED,
                    OutcomeType.NECESSARY_USE -> false
                    OutcomeType.PURPOSE_DRIFT,
                    OutcomeType.CONTINUED_SCROLLING -> true
                    else -> null
                },
                closedAfterIntervention = closedAfterIntervention.takeIf { it },
                interventionExitConfirmedAtMillis = if (closedAfterIntervention) endedAt else null,
                overrun = rawOverrunMillis > 0L,
                extensionCount = extensionCount,
                cautionExtensionCount = cautionExtensionCount,
                interventionEverApplied = interventionEverApplied,
                isFastReopen = key in setOf("S05", "S10", "S16"),
                reopenGapMillis = when (key) {
                    "S05", "S16" -> THREE_MINUTES
                    "S10" -> FIVE_MINUTES
                    else -> null
                },
                createdAtMillis = outcomeRespondedAtMillis ?: endedAt
            ),
            extensionEvents = extensionEvents
        )
    }

    private fun selfControl(
        packageName: String,
        appDisplayName: String,
        occurredAtMillis: Long,
        interventionActive: Boolean
    ): SelfControlEventEntity = SelfControlEventEntity(
        packageName = packageName,
        appDisplayName = appDisplayName,
        eventType = SessionLogRepository.EVENT_CLOSE_NOW_BEFORE_SESSION,
        occurredAtMillis = occurredAtMillis,
        modeAtTime = InterventionMode.INTERVENTION.name,
        isVulnerableTimeAtTime = interventionActive,
        interventionActiveAtTime = interventionActive,
        source = "intent_close_now"
    )

    private fun interventionVisibleMillis(
        extensionCount: Int,
        closedAfterIntervention: Boolean,
        interventionEverApplied: Boolean
    ): Long {
        val brakeCount = maxOf(extensionCount, if (closedAfterIntervention || interventionEverApplied) 1 else 0)
        return brakeCount * BRAKE_VISIBLE_MILLIS
    }

    private fun todaySchedule(nowMillis: Long): TodaySchedule {
        val closeNowMillis = nowMillis - CLOSE_NOW_BEFORE_NOW_MILLIS
        val s17EndMillis = closeNowMillis - GAP_AFTER_TODAY_LAST_SESSION_MILLIS
        val s17EntryMillis = s17EndMillis - START_DELAY_MILLIS - TODAY_S17_DURATION_MILLIS
        val s16EndMillis = s17EntryMillis - GAP_BETWEEN_TODAY_SESSIONS_MILLIS
        val s16EntryMillis = s16EndMillis - START_DELAY_MILLIS - TODAY_S16_DURATION_MILLIS
        val s15EndMillis = s16EntryMillis - THREE_MINUTES
        val s15EntryMillis = s15EndMillis - START_DELAY_MILLIS - TODAY_S15_DURATION_MILLIS
        return TodaySchedule(
            s15EntryMillis = s15EntryMillis,
            s16EntryMillis = s16EntryMillis,
            s17EntryMillis = s17EntryMillis,
            closeNowMillis = closeNowMillis
        )
    }

    private fun startOfTodayMillis(
        nowMillis: Long,
        timeZone: TimeZone
    ): Long = Calendar.getInstance(timeZone).apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun nowMillisFromTodayScheduleOffset(): Long =
        CLOSE_NOW_BEFORE_NOW_MILLIS +
            GAP_AFTER_TODAY_LAST_SESSION_MILLIS +
            START_DELAY_MILLIS +
            TODAY_S17_DURATION_MILLIS +
            GAP_BETWEEN_TODAY_SESSIONS_MILLIS +
            START_DELAY_MILLIS +
            TODAY_S16_DURATION_MILLIS +
            THREE_MINUTES +
            START_DELAY_MILLIS +
            TODAY_S15_DURATION_MILLIS

    private data class TodaySchedule(
        val s15EntryMillis: Long,
        val s16EntryMillis: Long,
        val s17EntryMillis: Long,
        val closeNowMillis: Long
    ) {
        val earliestTimestampMillis: Long
            get() = s15EntryMillis
    }

    private class DemoDayClock(
        nowMillis: Long,
        private val timeZone: TimeZone
    ) {
        private val startOfToday = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        fun at(dayOffset: Int, hour: Int, minute: Int): Long =
            (startOfToday.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
    }

    private const val YOUTUBE = DemoToolsContract.YOUTUBE_PACKAGE
    private const val INSTAGRAM = DemoToolsContract.INSTAGRAM_PACKAGE
    private const val CHROME = DemoToolsContract.CHROME_PACKAGE
    private const val START_DELAY_MILLIS = 2_000L
    private const val OUTCOME_RESPONSE_DELAY_MILLIS = 1_000L
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val THREE_MINUTES = 3L * MILLIS_PER_MINUTE
    private const val FIVE_MINUTES = 5L * MILLIS_PER_MINUTE
    private const val BRAKE_VISIBLE_MILLIS = 12_000L
    private const val CLOSE_NOW_BEFORE_NOW_MILLIS = 2L * MILLIS_PER_MINUTE
    private const val GAP_AFTER_TODAY_LAST_SESSION_MILLIS = 2L * MILLIS_PER_MINUTE
    private const val GAP_BETWEEN_TODAY_SESSIONS_MILLIS = 2L * MILLIS_PER_MINUTE
    private const val TODAY_S15_DURATION_MILLIS = 4L * MILLIS_PER_MINUTE
    private const val TODAY_S16_DURATION_MILLIS = 5L * MILLIS_PER_MINUTE + BRAKE_VISIBLE_MILLIS
    private const val TODAY_S17_DURATION_MILLIS = 6L * MILLIS_PER_MINUTE
}
