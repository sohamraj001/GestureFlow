package com.handsfree.scroll.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object GestureEventBus {
    private val _events = MutableSharedFlow<GestureEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    enum class GestureEvent {
        SWIPE_UP,
        SWIPE_DOWN,
        LIKE_GESTURE,
        PAUSE_PLAY_GESTURE
    }

    suspend fun emit(event: GestureEvent) {
        _events.emit(event)
    }
}
