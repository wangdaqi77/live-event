package androidx.lifecycle

/**
 * [NoStickyBackgroundLiveEvent] which override [observe] and [observeForever] method,
 * they actually map [observeNoSticky] and [observeForeverNoSticky] method.
 *
 * @param T The type of data hold by this instance.
 */
open class NoStickyBackgroundLiveEvent<T> : BackgroundLiveEvent<T> {

    /**
     * Creates a NoStickyBackgroundLiveEvent initialized with the given value.
     *
     * @property value initial value.
     */
    constructor(value: T) : super(value)

    /**
     * Creates a NoStickyBackgroundLiveEvent with no value assigned to it.
     */
    constructor() : super()

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observeNoSticky(owner, observer)
    }

    override fun observeForever(observer: Observer<in T>) {
        super.observeForeverNoSticky(observer)
    }
}