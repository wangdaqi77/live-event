package com.data.demo

import androidx.lifecycle.BackgroundLiveEvent
import androidx.lifecycle.LiveEvent
import com.data.demo.BaseDemoActivity


class LikeLiveDataDemoActivity : BaseDemoActivity() {
    override val liveEvent: LiveEvent<String> = LiveEvent()
    override val backgroundLiveEvent: BackgroundLiveEvent<String> = BackgroundLiveEvent()
}