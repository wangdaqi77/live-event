package wang.lifecycle.internal

import androidx.arch.core.internal.SafeIterableMap
import androidx.lifecycle.*
import androidx.lifecycle.LiveData
import java.lang.IllegalStateException
import java.lang.reflect.Field
import java.lang.reflect.Method

internal object InternalReflect {

    private var mObserversFieldAccessible = false
    private var mDispatchingValueFieldAccessible = false
    private var considerNotifyMethodAccessible = false
    private var mEnforceMainThreadAccessible = false

    private val LiveData_class : Class<*> = LiveData::class.java
    private val LifecycleRegistry_class : Class<*> = LifecycleRegistry::class.java

    private val mEnforceMainThread_field : Field? by lazy(LazyThreadSafetyMode.NONE) {
        try {
            // lifecycle-runtime-2.3.1
            // private final boolean mEnforceMainThread;
            LifecycleRegistry_class.getDeclaredField("mEnforceMainThread").also {
                mEnforceMainThreadAccessible = it.isAccessible
            }
        }catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val mObservers_field : Field by lazy(LazyThreadSafetyMode.NONE) {
        // private SafeIterableMap<Observer<? super T>, ObserverWrapper> mObservers =
        //            new SafeIterableMap<>();
        LiveData_class.getDeclaredField("mObservers").also {
            mObserversFieldAccessible = it.isAccessible
        }
    }

    private val mDispatchingValue_field : Field by lazy(LazyThreadSafetyMode.NONE) {
        // private boolean mDispatchingValue;
        LiveData_class.getDeclaredField("mDispatchingValue").also {
            mDispatchingValueFieldAccessible = it.isAccessible
        }
    }

    private val considerNotify_method : Method by lazy(LazyThreadSafetyMode.NONE) {
        // private void considerNotify(ObserverWrapper observer)
        val method = LiveData_class.declaredMethods.findLast { it.name == "considerNotify"}?.also{
            considerNotifyMethodAccessible = it.isAccessible
        }
        method ?: throw IllegalStateException("must config proguard rules!")
    }

    fun <T> getObservers(liveData: InternalSupportedLiveData<T>): SafeIterableMap<Observer<in T>, *>{
        mObservers_field.isAccessible = true
        val mObservers = mObservers_field.get(liveData)
        mObservers_field.isAccessible =
            mObserversFieldAccessible
        @Suppress("UNCHECKED_CAST")
        return mObservers as SafeIterableMap<Observer<in T>, *>
    }

    fun <T> mDispatchingValue(liveData: InternalSupportedLiveData<T>): Boolean {
        mDispatchingValue_field.isAccessible = true
        val mDispatchingValue = mDispatchingValue_field.get(liveData) as Boolean
        mDispatchingValue_field.isAccessible = mDispatchingValueFieldAccessible
        return mDispatchingValue
    }

    fun <T> considerNotify(liveData: InternalSupportedLiveData<T>, observerWrapper: Any) {
        considerNotify_method.isAccessible = true
        considerNotify_method.invoke(liveData, observerWrapper)
        considerNotify_method.isAccessible = considerNotifyMethodAccessible
    }

    fun closeCheckManiThreadOfLifecycleRegistry(lifecycle: LifecycleRegistry) {
        mEnforceMainThread_field?.apply {
            isAccessible = true
            set(lifecycle, false)
            isAccessible = mEnforceMainThreadAccessible
        }
    }

}