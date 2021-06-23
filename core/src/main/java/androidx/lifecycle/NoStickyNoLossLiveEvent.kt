package androidx.lifecycle

/**
 * [NoStickyNoLossLiveEvent] which override [observe] and [observeForever] method,
 * they actually map [observeNoStickyNoLoss] and [observeForeverNoStickyNoLoss] method.
 *
 * @param T The type of data hold by this instance.
 */
open class NoStickyNoLossLiveEvent<T> : LiveEvent<T> {

    /**
     * Creates a NoStickyNoLossLiveEvent initialized with the given value.
     *
     * @property value initial value.
     */
    constructor(value: T) : super(value)

    /**
     * Creates a NoStickyNoLossLiveEvent with no value assigned to it.
     */
    constructor() : super()

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observeNoStickyNoLoss(owner, observer)
    }

    override fun observeForever(observer: Observer<in T>) {
        super.observeForeverNoStickyNoLoss(observer)
    }
}