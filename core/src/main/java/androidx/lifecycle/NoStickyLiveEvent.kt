package androidx.lifecycle

/**
 * [NoStickyLiveEvent] which override [observe] and [observeForever] method,
 * they actually map [observeNoSticky] and [observeForeverNoSticky] method.
 *
 * @param T The type of data hold by this instance.
 */
open class NoStickyLiveEvent<T> : LiveEvent<T> {

    /**
     * Creates a NoStickyLiveEvent initialized with the given value.
     *
     * @property value initial value.
     */
    constructor(value: T) : super(value)

    /**
     * Creates a NoStickyLiveEvent with no value assigned to it.
     */
    constructor() : super()

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observeNoSticky(owner, observer)
    }

    override fun observeForever(observer: Observer<in T>) {
        super.observeForeverNoSticky(observer)
    }
}