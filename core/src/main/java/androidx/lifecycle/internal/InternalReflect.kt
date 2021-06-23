package androidx.lifecycle.internal

import androidx.arch.core.internal.SafeIterableMap
import androidx.lifecycle.*
import java.lang.IllegalStateException
import java.lang.reflect.Field
import java.lang.reflect.Method

internal object InternalReflect {
    const val START_VERSION = -1
    const val ERROR_MESSAGE = ""

    private var mPendingDataFieldAccessible = false
    private var mDataLockFieldAccessible = false
    private var mVersionFieldAccessible = false
    private var mObserversFieldAccessible = false
    private var mDispatchingValueFieldAccessible = false
    private var considerNotifyMethodAccessible = false
    private var mEnforceMainThreadAccessible = false

    private val LiveData_class : Class<*> = LiveData::class.java
    private val LifecycleRegistry_class : Class<*> = LifecycleRegistry::class.java

    val NOT_SET : Any by lazy(LazyThreadSafetyMode.NONE) {
        // static final Object NOT_SET = new Object();
        val field = LiveData_class.getDeclaredField("NOT_SET")
        val oldAccessible = field.isAccessible
        field.isAccessible = true
        val value = field.get(LiveData_class)
        field.isAccessible = oldAccessible
        value!!
    }

    private val mDataLock_field : Field by lazy(LazyThreadSafetyMode.NONE) {
        // final Object mDataLock = new Object();
        LiveData_class.getDeclaredField("mDataLock").also {
            mDataLockFieldAccessible = it.isAccessible
        }
    }

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

    private val mPendingData_field : Field by lazy(LazyThreadSafetyMode.NONE) {
        // volatile Object mPendingData = NOT_SET;
        LiveData_class.getDeclaredField("mPendingData").also {
            mPendingDataFieldAccessible = it.isAccessible
        }
    }

    private val mVersion_field : Field by lazy(LazyThreadSafetyMode.NONE) {
        // private int mVersion;
        LiveData_class.getDeclaredField("mVersion").also {
            mVersionFieldAccessible = it.isAccessible
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

    private lateinit var mLastVersion_field: Field

    fun <T> getDataLock(liveData: LiveData<T>): Any {
        mDataLock_field.isAccessible = true
        val dataLock = mDataLock_field.get(liveData)
        mDataLock_field.isAccessible = mDataLockFieldAccessible
        return dataLock!!
    }

    fun <T> getPendingData(liveData: LiveData<T>): Any? {
        mPendingData_field.isAccessible = true
        val mPendingData = mPendingData_field.get(liveData)
        mPendingData_field.isAccessible = mPendingDataFieldAccessible
        return mPendingData
    }

    fun <T> getObservers(liveData: LiveData<T>): SafeIterableMap<Observer<in T>, *>{
        mObservers_field.isAccessible = true
        val mObservers = mObservers_field.get(liveData)
        mObservers_field.isAccessible =
            mObserversFieldAccessible
        @Suppress("UNCHECKED_CAST")
        return mObservers as SafeIterableMap<Observer<in T>, *>
    }

    fun <T> getVersion(liveData: LiveData<T>): Int {
        mVersion_field.isAccessible = true
        val mVersion = mVersion_field.get(liveData) as Int
        mVersion_field.isAccessible = mVersionFieldAccessible
        return mVersion
    }

    fun <T> mDispatchingValue(liveData: LiveData<T>): Boolean {
        mDispatchingValue_field.isAccessible = true
        val mDispatchingValue = mDispatchingValue_field.get(liveData) as Boolean
        mDispatchingValue_field.isAccessible = mDispatchingValueFieldAccessible
        return mDispatchingValue
    }

    fun <T> considerNotify(liveData: LiveData<T>, observerWrapper: Any) {
        considerNotify_method.isAccessible = true
        considerNotify_method.invoke(liveData, observerWrapper)
        considerNotify_method.isAccessible = considerNotifyMethodAccessible
    }

    fun addObserverSafe(lifecycleOwner: LifecycleOwner, lifecycleEventObserver: LifecycleEventObserver) {
        mEnforceMainThread_field.apply {
            if (this != null) {
                val lifecycle = lifecycleOwner.lifecycle
                if (lifecycle is LifecycleRegistry) {
                    isAccessible = true

                    val old = get(lifecycle)
                    set(lifecycle, false)

                    val addIfFailedRetry = addIfFailedRetry@{ count : Int ->
                        for (i in 1 .. count) {
                            try {
                                lifecycleOwner.lifecycle.addObserver(lifecycleEventObserver)
                                return@addIfFailedRetry
                            }catch (e: Exception) {
                                if (i == count) throw e
                            }
                        }

                    }
                    addIfFailedRetry(5)

                    set(lifecycle, old)

                    // isAccessible = mEnforceMainThreadAccessible
                }
            }else {
                lifecycleOwner.lifecycle.addObserver(lifecycleEventObserver)
            }
        }
    }


    fun removeObserverSafe(lifecycleOwner: LifecycleOwner, lifecycleEventObserver: LifecycleEventObserver) {
        mEnforceMainThread_field.apply {
            if (this != null) {
                val lifecycle = lifecycleOwner.lifecycle
                if (lifecycle is LifecycleRegistry) {
                    isAccessible = true

                    val old = get(lifecycle)
                    set(lifecycle, false)

                    val removeIfFailedRetry = removeIfFailedRetry@{ count : Int ->
                        for (i in 1 .. count) {
                            try {
                                lifecycleOwner.lifecycle.removeObserver(lifecycleEventObserver)
                                return@removeIfFailedRetry
                            }catch (e: Exception) {
                                if (i == count) throw e
                            }
                        }

                    }
                    removeIfFailedRetry(5)

                    set(lifecycle, old)

                    // isAccessible = mEnforceMainThreadAccessible
                }
            }else {
                lifecycleOwner.lifecycle.removeObserver(lifecycleEventObserver)
            }
        }
    }


    @Suppress("LocalVariableName")
    fun syncVersion(liveData: LiveData<*>, observerWrapper: Any) {
        var mLastVersionFieldAccessible = false
        try {

            // ObserverWrapper.mLastVersion field
            if (!InternalReflect::mLastVersion_field.isInitialized) {
                // ObserverWrapper.class
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                val wrapperClass: Class<*> = observerWrapper.javaClass.superclass
                // ObserverWrapper.mLastVersion
                mLastVersion_field = wrapperClass.getDeclaredField("mLastVersion")
            }

            mLastVersionFieldAccessible = mLastVersion_field.isAccessible
            mLastVersion_field.isAccessible = true

            // ObserverWrapper.mLastVersion = ListData.mVersion
            mLastVersion_field[observerWrapper] = getVersion(liveData)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }finally {
            mLastVersion_field.isAccessible = mLastVersionFieldAccessible
        }
    }

}