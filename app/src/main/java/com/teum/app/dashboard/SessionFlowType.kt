package com.teum.app.dashboard

enum class SessionPrimaryFlowType {
    CONTINUED_SCROLLING,
    PURPOSE_DRIFTED,
    EXTENDED_AFTER_BRAKE,
    REACHED_AND_CLOSED,
    ENDED_BEFORE_TARGET,
    COMPLETED,
    UNKNOWN
}

enum class SessionFlowBadge {
    FAST_REOPENED,
    UNCONSCIOUS_OPEN,
    MINDFUL_REST,
    CLEAR_PURPOSE,
    PURPOSE_KEPT,
    NECESSARY_USE,
    RAW_OVER_TARGET,
    MULTIPLE_EXTENSIONS
}

enum class SessionFlowTone {
    POSITIVE,
    NEUTRAL,
    ATTENTION,
    WARNING
}
