package com.data.demo.mutable

import androidx.lifecycle.MutableBackgroundLiveEvent
import androidx.lifecycle.MutableLiveEvent
import androidx.lifecycle.mutable.NoLossBackgroundLiveEvent
import androidx.lifecycle.mutable.NoLossLiveEvent
import com.data.demo.BaseMutableDemoActivity


class NoLossDemoActivity : BaseMutableDemoActivity() {
    override val liveEvent: MutableLiveEvent<String> = NoLossLiveEvent()
    override val backgroundLiveEvent: MutableBackgroundLiveEvent<String> = NoLossBackgroundLiveEvent()
}