package com.teum.app.debug

import android.util.Log

object TeumLogger {
    private const val TAG_FLOW = "TeumFlow"

    fun flow(message: String) {
        Log.d(TAG_FLOW, message)
    }

    fun session(debugSessionId: Long, event: String, detail: String = "") {
        val suffix = detail.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
        flow("[S#$debugSessionId] $event$suffix")
    }

    fun access(event: String, packageName: String) {
        flow("[ACCESS] $event package=$packageName")
    }

    fun reopen(message: String) {
        flow("[REOPEN] $message")
    }

    fun overlay(event: String, detail: String = "") {
        val suffix = detail.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
        flow("[OVERLAY] $event$suffix")
    }
}
