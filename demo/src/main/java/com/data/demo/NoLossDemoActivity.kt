package com.data.demo

import androidx.lifecycle.BackgroundLiveEvent
import androidx.lifecycle.LiveEvent
import androidx.lifecycle.NoLossBackgroundLiveEvent
import androidx.lifecycle.NoLossLiveEvent
import com.data.demo.BaseDemoActivity


class NoLossDemoActivity : BaseDemoActivity() {
    override val liveEvent: LiveEvent<String> =
        NoLossLiveEvent()
    override val backgroundLiveEvent: BackgroundLiveEvent<String> =
        NoLossBackgroundLiveEvent()
}