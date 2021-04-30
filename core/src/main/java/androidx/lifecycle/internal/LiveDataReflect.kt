package androidx.lifecycle.internal

import androidx.arch.core.internal.SafeIterableMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.lang.reflect.Field

internal object LiveDataReflect {
    const val START_VERSION = -1

    private var mPendingDataFieldAccessible = false
    private var mDataLockFieldAccessible = false
    private var mVersionFieldAccessible = false
    private var mObserversFieldAccessible = false

    private val LiveData_class : Class<*> = LiveData::class.java

    val NOT_SET : Any by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // static final Object NOT_SET = new Object();
        val field = LiveData_class.getDeclaredField("NOT_SET")
        val oldAccessible = field.isAccessible
        field.isAccessible = true
        val value = field.get(LiveData_class)
        field.isAccessible = oldAccessible
        value!!
    }

    private val mDataLock_field : Field by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // final Object mDataLock = new Object();
        LiveData_class.getDeclaredField("mDataLock").also {
            mDataLockFieldAccessible = it.isAccessible
        }
    }

    private val mObservers_field : Field by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // private SafeIterableMap<Observer<? super T>, ObserverWrapper> mObservers =
        //            new SafeIterableMap<>();
        LiveData_class.getDeclaredField("mObservers").also {
            mObserversFieldAccessible = it.isAccessible
        }
    }

    private val mPendingData_field : Field by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // volatile Object mPendingData = NOT_SET;
        LiveData_class.getDeclaredField("mPendingData").also {
            mPendingDataFieldAccessible = it.isAccessible
        }
    }

    private val mVersion_field : Field by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // private int mVersion;
        LiveData_class.getDeclaredField("mVersion").also {
            mVersionFieldAccessible = it.isAccessible
        }
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

    @Suppress("LocalVariableName")
    fun syncVersion(liveData: LiveData<*>, observerWrapper: Any) {
        var mLastVersionFieldAccessible = false
        try {

            // ObserverWrapper.mLastVersion field
            if (!LiveDataReflect::mLastVersion_field.isInitialized) {
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