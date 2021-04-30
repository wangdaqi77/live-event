package androidx.lifecycle

/**
 * [MutableBackgroundLiveEvent] which publicly exposes [setValue] and [postValue] method.
 *
 * @param T The type of data hold by this instance
 */
open class MutableBackgroundLiveEvent<T> : BackgroundLiveEvent<T> {

    /**
     * Creates a MutableBackgroundLiveEvent initialized with the given value.
     *
     * @property value initial value
     */
    constructor(value: T) : super(value)

    /**
     * Creates a MutableBackgroundLiveEvent with no value assigned to it.
     */
    constructor() : super()

    public override fun postValue(value: T) {
        super.postValue(value)
    }

    public override fun setValue(value: T) {
        super.setValue(value)
    }
}