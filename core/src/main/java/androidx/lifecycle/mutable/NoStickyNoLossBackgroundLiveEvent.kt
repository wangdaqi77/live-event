package androidx.lifecycle.mutable

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableBackgroundLiveEvent
import androidx.lifecycle.Observer

/**
 * [NoStickyNoLossBackgroundLiveEvent] which override [observe] and [observeForever] method,
 * they actually map [observeNoStickyNoLoss] and [observeForeverNoStickyNoLoss] method.
 *
 * @param T The type of data hold by this instance
 */
open class NoStickyNoLossBackgroundLiveEvent<T> :
    MutableBackgroundLiveEvent<T> {

    /**
     * Creates a NoStickyNoLossBackgroundLiveEvent initialized with the given value.
     *
     * @property value initial value
     */
    constructor(value: T) : super(value)

    /**
     * Creates a NoStickyNoLossBackgroundLiveEvent with no value assigned to it.
     */
    constructor() : super()

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observeNoStickyNoLoss(owner, observer)
    }

    override fun observeForever(observer: Observer<in T>) {
        super.observeForeverNoStickyNoLoss(observer)
    }
}