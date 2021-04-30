package com.data.demo.mutable

import androidx.lifecycle.MutableBackgroundLiveEvent
import androidx.lifecycle.MutableLiveEvent
import com.data.demo.BaseMutableDemoActivity


class LikeLiveDataDemoActivity : BaseMutableDemoActivity() {
    override val liveEvent: MutableLiveEvent<String> = MutableLiveEvent()
    override val backgroundLiveEvent: MutableBackgroundLiveEvent<String> = MutableBackgroundLiveEvent()
}