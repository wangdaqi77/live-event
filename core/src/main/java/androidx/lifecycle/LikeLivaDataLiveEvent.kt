package androidx.lifecycle

/**
 * [LikeLiveDataLiveEvent] also is [LiveData].
 *
 * @param T The type of data hold by this instance.
 */
open class LikeLiveDataLiveEvent<T> : LiveEvent<T>{

    /**
     * Creates a LikeLiveDataLiveEvent initialized with the given value.
     *
     * @property value initial value.
     */
    constructor(value: T) : super(value)

    /**
     * Creates a LikeLiveDataLiveEvent with no value assigned to it.
     */
    constructor() : super()
}