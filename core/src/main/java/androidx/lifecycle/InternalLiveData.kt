package androidx.lifecycle

import wang.lifecycle.internal.InternalMainExecutor
import wang.lifecycle.internal.InternalReflect

open class InternalLiveData<T> : LiveData<T> {

    internal companion object {
        val START_VERSION : Int
            get() = LiveData.START_VERSION
        val NOT_SET: Any
            get() = LiveData.NOT_SET

        fun assertMainThread(methodName: String){
            if (!InternalMainExecutor.isMainThread) {
                throw IllegalStateException(
                    "Cannot invoke " + methodName + " on a background"
                            + " thread"
                )
            }
        }

    }

    internal val mDataLock : Any
        get() = super.mDataLock

    internal var mPendingData: Any?
        get() = super.mPendingData
        set(value) {
            super.mPendingData = value
        }

    internal val version : Int
        get() = super.getVersion()

    internal val mObservers by lazy(LazyThreadSafetyMode.NONE) { InternalReflect.getObservers(this) }

    internal val mDispatchingValue
        get() = InternalReflect.mDispatchingValue(this)

    internal constructor() : super()

    internal constructor(value: T) : super(value)

    internal fun syncVersion(observer: Observer<T>) {
        @Suppress("UNCHECKED_CAST")
        val value = mObservers.putIfAbsent(observer, null) as LiveData<T>.LifecycleBoundObserver
        value.mLastVersion = version
    }

}