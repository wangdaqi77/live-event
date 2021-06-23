package androidx.lifecycle.mixed

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveEvent
import androidx.lifecycle.Observer

/**
 * [MixedLiveEvent] publicly exposes all observe method.
 *
 * @param T The type of data hold by this instance.
 */
open class MixedLiveEvent<T> : LiveEvent<T> {

    /**
     * Creates a MixedLiveEvent initialized with the given value.
     *
     * @property value initial value.
     */
    constructor(value: T) : super(value)

    /**
     * Creates a MixedLiveEvent with no value assigned to it.
     */
    constructor() : super()

    public override fun postValue(value: T) {
        super.postValue(value)
    }

    public override fun setValue(value: T) {
        super.setValue(value)
    }

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner, observer)
    }

    override fun observeForever(observer: Observer<in T>) {
        super.observeForever(observer)
    }

    public override fun observeNoSticky(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observeNoSticky(owner, observer)
    }

    public override fun observeForeverNoSticky(observer: Observer<in T>) {
        super.observeForeverNoSticky(observer)
    }

    public override fun observeNoLoss(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observeNoLoss(owner, observer)
    }

    public override fun observeForeverNoLoss(observer: Observer<in T>) {
        super.observeForeverNoLoss(observer)
    }

    public override fun observeNoStickyNoLoss(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observeNoStickyNoLoss(owner, observer)
    }

    public override fun observeForeverNoStickyNoLoss(observer: Observer<in T>) {
        super.observeForeverNoStickyNoLoss(observer)
    }
}