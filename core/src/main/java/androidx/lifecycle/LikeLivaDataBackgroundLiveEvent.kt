package androidx.lifecycle

/**
 * [LikeLiveDataBackgroundLiveEvent] like [LiveData].
 *
 * @param T The type of data hold by this instance.
 */
open class LikeLiveDataBackgroundLiveEvent<T> : BackgroundLiveEvent<T> {

    /**
     * Creates a LikeLiveDataBackgroundLiveEvent initialized with the given value.
     *
     * @property value initial value.
     */
    constructor(value: T) : super(value)

    /**
     * Creates a LikeLiveDataBackgroundLiveEvent with no value assigned to it.
     */
    constructor() : super()
}