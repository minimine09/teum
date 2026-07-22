package com.teum.app.core.model

enum class InterventionMode(
    val label: String,
    val description: String
) {
    LOW(
        label = "약함",
        description = "짧은 확인 위주로 흐름을 덜 끊어요."
    ),
    NORMAL(
        label = "보통",
        description = "현재 기준의 Intent Check와 Session Brake를 사용해요."
    ),
    HIGH(
        label = "강함",
        description = "반복 실행과 초과 사용을 더 강하게 막는 방향이에요."
    )
}
