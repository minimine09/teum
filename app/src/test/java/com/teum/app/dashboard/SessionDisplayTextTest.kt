package com.teum.app.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionDisplayTextTest {
    @Test
    fun intentValuesUseKoreanUserLabels() {
        assertEquals("명확한 목적", SessionDisplayText.intent("CLEAR_PURPOSE"))
        assertEquals("인지하고 선택한 휴식", SessionDisplayText.intent("MINDFUL_REST"))
        assertEquals("무의식적으로 실행", SessionDisplayText.intent("UNCONSCIOUS_OPEN"))
        assertEquals("사용하지 않고 닫기", SessionDisplayText.intent("CLOSE_NOW"))
        assertEquals("확인되지 않은 목적", SessionDisplayText.intent("UNKNOWN"))
    }

    @Test
    fun outcomePrefersOutcomeCheckAnswersAndHandlesMissingValue() {
        assertEquals("목표를 달성했어요", SessionDisplayText.outcome("PURPOSE_ACHIEVED", null, null))
        assertEquals("필요한 사용으로 확인했어요", SessionDisplayText.outcome("NECESSARY_USE", null, null))
        assertEquals("목적에서 벗어났어요", SessionDisplayText.outcome("PURPOSE_DRIFT", null, null))
        assertEquals(
            "무의식적으로 계속 사용했어요",
            SessionDisplayText.outcome("CONTINUED_SCROLLING", null, null)
        )
        assertEquals("사용 시간을 연장했어요", SessionDisplayText.outcome("EXTENDED", null, null))
        assertEquals("목표를 달성했어요", SessionDisplayText.outcome(null, true, false))
        assertEquals("목적에서 벗어났어요", SessionDisplayText.outcome("ENDED", false, true))
        assertEquals("사용 결과를 아직 확인하지 않았어요", SessionDisplayText.outcome(null, null, null))
    }

    @Test
    fun overrunUsesFriendlyTextForSubSecondDurations() {
        assertEquals("목표 시간 안에 종료했어요", SessionDisplayText.overrun(0L))
        assertEquals("목표 시간을 1초 미만 초과했어요", SessionDisplayText.overrun(37L))
        assertEquals("목표 시간을 1분 5초 초과했어요", SessionDisplayText.overrun(65_000L))
    }
}
