package com.teum.app.dashboard

import com.teum.app.data.local.entity.SessionLogEntity

/**
 * User-facing initial goal policy.
 *
 * Reaching Session Brake means the initial goal was reached. The session is
 * considered to have exceeded that goal only when the user explicitly extends it.
 * Raw/final allowance overrun remains available separately through SessionMetrics.
 */
object SessionGoalPolicy {
    fun keptInitialGoal(session: SessionLogEntity): Boolean =
        !exceededInitialGoal(session)

    fun exceededInitialGoal(session: SessionLogEntity): Boolean =
        session.extensionCount > 0
}
