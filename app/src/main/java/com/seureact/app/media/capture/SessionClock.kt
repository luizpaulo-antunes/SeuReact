package com.seureact.app.media.capture

import android.os.SystemClock

class SessionClock(private val nowNanos: () -> Long = SystemClock::elapsedRealtimeNanos) {
    private var originNanos: Long? = null
    fun start(): Long = nowNanos().also { originNanos = it }
    fun elapsedNanos(): Long = nowNanos() - checkNotNull(originNanos) { "Relógio não iniciado" }
}
