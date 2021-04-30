package com.data.demo.mutable

import androidx.lifecycle.MutableBackgroundLiveEvent
import androidx.lifecycle.MutableLiveEvent
import androidx.lifecycle.mutable.NoStickyBackgroundLiveEvent
import androidx.lifecycle.mutable.NoStickyLiveEvent
import com.data.demo.BaseMutableDemoActivity


class NoStickyDemoActivity : BaseMutableDemoActivity() {
    override val liveEvent: MutableLiveEvent<String> = NoStickyLiveEvent()
    override val backgroundLiveEvent: MutableBackgroundLiveEvent<String> = NoStickyBackgroundLiveEvent()
}