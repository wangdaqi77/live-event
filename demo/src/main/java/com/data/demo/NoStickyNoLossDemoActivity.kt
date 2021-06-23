package com.data.demo

import androidx.lifecycle.BackgroundLiveEvent
import androidx.lifecycle.LiveEvent
import androidx.lifecycle.NoStickyNoLossBackgroundLiveEvent
import androidx.lifecycle.NoStickyNoLossLiveEvent
import com.data.demo.BaseDemoActivity

class NoStickyNoLossDemoActivity : BaseDemoActivity() {
    override val liveEvent: LiveEvent<String> =
        NoStickyNoLossLiveEvent()
    override val backgroundLiveEvent: BackgroundLiveEvent<String> =
        NoStickyNoLossBackgroundLiveEvent()
}