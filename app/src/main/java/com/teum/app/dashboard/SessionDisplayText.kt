package com.teum.app.dashboard

object SessionDisplayText {
    fun intent(intentChoice: String): String = when (intentChoice) {
        "CLEAR_PURPOSE" -> "명확한 목적"
        "MINDFUL_REST" -> "인지하고 선택한 휴식"
        "UNCONSCIOUS_OPEN" -> "무의식적으로 실행"
        "CLOSE_NOW" -> "사용하지 않고 닫기"
        else -> "확인되지 않은 목적"
    }

    fun outcome(
        outcomeType: String?,
        outcomeAchieved: Boolean?,
        purposeDrifted: Boolean?
    ): String = when {
        outcomeAchieved == true -> "목표를 달성했어요"
        purposeDrifted == true -> "목적에서 벗어났어요"
        outcomeType == "ENDED" -> "사용을 종료했어요"
        outcomeType == "EXTENDED" -> "사용 시간을 연장했어요"
        outcomeType == "NECESSARY_USE" -> "필요한 사용으로 확인했어요"
        outcomeType == "PURPOSE_DRIFT" -> "목적에서 벗어났어요"
        outcomeType == null -> "사용 결과를 아직 확인하지 않았어요"
        else -> "사용 결과를 확인할 수 없어요"
    }

    fun overrun(overrunMillis: Long): String = when {
        overrunMillis <= 0L -> "목표 시간 안에 종료했어요"
        overrunMillis < 1_000L -> "목표 시간을 1초 미만 초과했어요"
        else -> "목표 시간을 ${formatDuration(overrunMillis)} 초과했어요"
    }

    private fun formatDuration(durationMillis: Long): String {
        val totalSeconds = (durationMillis / 1_000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return when {
            minutes > 0L && seconds > 0L -> "${minutes}분 ${seconds}초"
            minutes > 0L -> "${minutes}분"
            else -> "${seconds}초"
        }
    }
}
