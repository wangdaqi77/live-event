package com.data.demo.mutable

import androidx.lifecycle.MutableBackgroundLiveEvent
import androidx.lifecycle.MutableLiveEvent
import androidx.lifecycle.mutable.NoStickyNoLossBackgroundLiveEvent
import androidx.lifecycle.mutable.NoStickyNoLossLiveEvent
import com.data.demo.BaseMutableDemoActivity

class NoStickyNoLossDemoActivity : BaseMutableDemoActivity() {
    override val liveEvent: MutableLiveEvent<String> = NoStickyNoLossLiveEvent()
    override val backgroundLiveEvent: MutableBackgroundLiveEvent<String> = NoStickyNoLossBackgroundLiveEvent()
}