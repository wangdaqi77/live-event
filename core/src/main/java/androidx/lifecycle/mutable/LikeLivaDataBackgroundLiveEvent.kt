package androidx.lifecycle.mutable

import androidx.lifecycle.MutableBackgroundLiveEvent
import androidx.lifecycle.LiveData

/**
 * [LikeLivaDataBackgroundLiveEvent] like [LiveData].
 *
 * @param T The type of data hold by this instance
 */
open class LikeLivaDataBackgroundLiveEvent<T> : MutableBackgroundLiveEvent<T> {

    /**
     * Creates a LikeLivaDataBackgroundLiveEvent initialized with the given value.
     *
     * @property value initial value
     */
    constructor(value: T) : super(value)

    /**
     * Creates a LikeLivaDataBackgroundLiveEvent with no value assigned to it.
     */
    constructor() : super()
}