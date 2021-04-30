package androidx.lifecycle.mutable

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveEvent

/**
 * [LikeLivaDataLiveEvent] like [LiveData].
 *
 * @param T The type of data hold by this instance
 */
open class LikeLivaDataLiveEvent<T> : MutableLiveEvent<T>{

    /**
     * Creates a LikeLivaDataLiveEvent initialized with the given value.
     *
     * @property value initial value
     */
    constructor(value: T) : super(value)

    /**
     * Creates a LikeLivaDataLiveEvent with no value assigned to it.
     */
    constructor() : super()
}