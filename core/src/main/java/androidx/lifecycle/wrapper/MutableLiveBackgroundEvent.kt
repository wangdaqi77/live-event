package androidx.lifecycle.wrapper

/**
 * [LiveBackgroundEvent] which publicly exposes [setValue] and [postValue] method.
 *
 * @param <T> The type of data hold by this instance
 */
open class MutableLiveBackgroundEvent<T> : LiveEvent<T> {

    /**
     * Creates a MutableLiveBackgroundEvent initialized with the given `value`.
     *
     * @param value initial value
     */
    constructor(value: T) : super(value)

    /**
     * Creates a MutableLiveBackgroundEvent with no value assigned to it.
     */
    constructor() : super()


    public override fun postValue(value: T) {
        super.postValue(value)
    }

    public override fun setValue(value: T) {
        super.setValue(value)
    }
}