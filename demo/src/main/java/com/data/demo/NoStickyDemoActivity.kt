package com.data.demo

import androidx.lifecycle.BackgroundLiveEvent
import androidx.lifecycle.LiveEvent
import androidx.lifecycle.NoStickyBackgroundLiveEvent
import androidx.lifecycle.NoStickyLiveEvent
import com.data.demo.BaseDemoActivity


class NoStickyDemoActivity : BaseDemoActivity() {
    override val liveEvent: LiveEvent<String> =
        NoStickyLiveEvent()
    override val backgroundLiveEvent: BackgroundLiveEvent<String> =
        NoStickyBackgroundLiveEvent()
}