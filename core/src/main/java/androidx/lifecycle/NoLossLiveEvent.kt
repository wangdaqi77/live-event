package androidx.lifecycle

/**
 * [NoLossLiveEvent] which override [observe] and [observeForever] method,
 * they actually map [observeNoLoss] and [observeForeverNoLoss] method.
 *
 * @param T The type of data hold by this instance.
 */
open class NoLossLiveEvent<T> : LiveEvent<T> {

    /**
     * Creates a NoLossLiveEvent initialized with the given value.
     *
     * @property value initial value.
     */
    constructor(value: T) : super(value)

    /**
     * Creates a NoLossLiveEvent with no value assigned to it.
     */
    constructor() : super()

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observeNoLoss(owner, observer)
    }

    override fun observeForever(observer: Observer<in T>) {
        super.observeForeverNoLoss(observer)
    }
}