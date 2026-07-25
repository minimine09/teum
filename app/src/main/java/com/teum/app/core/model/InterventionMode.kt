package com.teum.app.core.model

enum class InterventionMode(
    val label: String,
    val description: String
) {
    NORMAL(
        label = "보통 모드",
        description = "기본적인 자기점검 흐름을 사용해요."
    ),
    INTERVENTION(
        label = "조심 모드",
        description = "취약 시간대에 연장과 사용 시간을 조금 줄여 더 단단한 틈을 만들어요."
    );

    val isIntervention: Boolean
        get() = this == INTERVENTION
}
