package androidx.lifecycle.wrapper

import android.os.Looper
import androidx.annotation.MainThread
import androidx.lifecycle.*
import androidx.lifecycle.wrapper.internal.*

open class LiveEvent<T> : LiveData<T>, Observers<T> {

    companion object{
        internal val NOT_SET by lazy { LiveDataReflect.NOT_SET }
        internal val START_VERSION by lazy { LiveDataReflect.START_VERSION }

        private fun assertMainThread(methodName: String) {
            val isMainThread = Looper.getMainLooper().thread === Thread.currentThread()
            if (!isMainThread) {
                throw IllegalStateException("Cannot invoke $methodName on a background thread")
            }
        }
    }

    public constructor() : super()

    public constructor(value: T) : super(value)

    private val mObservers  by lazy {
        @Suppress("UNCHECKED_CAST")
        LiveDataReflect.getObservers(this)
    }
    private val mDataLock: Any by lazy { LiveDataReflect.getDataLock(this) }
    private val mVersion: Int
        get() = LiveDataReflect.getVersion(this)
    private val mPendingData: Any?
        get() = LiveDataReflect.getPendingData(this)
    private var mHasSet = false
    @Volatile
    private var mPossibleLostPendingValueHead : LostValue.Head<T>? = null

    /**
     * [observe] is a sticky function, this function is to shield the sticky.
     *
     * @see observe
     */
    @MainThread
    open fun observeNoSticky(owner: LifecycleOwner, observer:Observer<in T>) {
        assertMainThread("observeNoSticky")
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            // ignore
            return
        }

        mObservers.eachObserverBox{
            if (observer === it.observer) {
                require(it is NoStickyObserverWrapper) {
                    "Cannot add the same observer with different future"
                }
                require(it.isAttachedTo(owner)) {
                    "Cannot add the same observer with different lifecycles"
                }
                return
            }
        }

        if(mHasSet || mVersion == START_VERSION) {
            val noStickyObserver = LifecycleBoundNoStickyObserverWrapper(this, owner, observer, false)
            owner.lifecycle.addObserver(noStickyObserver)
            observeInner(owner, noStickyObserver)
            return
        }

        mHasSet = true

        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            val noStickyObserver = LifecycleBoundNoStickyObserverWrapper(this, owner, observer, true)
            owner.lifecycle.addObserver(noStickyObserver)
            observeInner(owner, noStickyObserver)
        }else{
            val noStickyObserver = LifecycleBoundNoStickyObserverWrapper(this, owner, observer, false)
            owner.lifecycle.addObserver(noStickyObserver)
            observeInner(owner, noStickyObserver)
            val observerWrapper = mObservers.putIfAbsent(noStickyObserver, null)
            LiveDataReflect.syncVersion(this, observerWrapper)
        }
    }

    /**
     * [observeForever] is a sticky function, this function is to shield the sticky.
     *
     * You should manually call [removeObserver] to stop observing this [LiveEvent].
     *
     * @see observeForever
     */
    @MainThread
    open fun observeForeverNoSticky(observer: Observer<in T>) {
        assertMainThread("observeForeverNoSticky")

        mObservers.eachObserverBox{
            if (observer === it.observer) {
                require(it is NoStickyObserverWrapper) {
                    "Cannot add the same observer with different future"
                }
                require(it is AlwaysActive) {
                    "Cannot add the same observer with different lifecycles"
                }
                return
            }
        }

        if(mHasSet || mVersion == START_VERSION) {
            val noStickyObserver = AlwaysActiveNoStickyObserverWrapper(observer, false)
            observeForeverInner(noStickyObserver)
            return
        }
        mHasSet = true

        val noStickyObserver = AlwaysActiveNoStickyObserverWrapper(observer, true)

        observeForeverInner(noStickyObserver)
    }

    /**
     * Using this function means that the [observer] no loss every value by calling [setValue]
     * and [postValue].
     *
     * If the [owner] be active, will record pending data when call [postValue], until next time
     * [setValue] is called, all recorded data will be notified to the [observer].
     * If the [owner] be inactive, will record data and pending data when call [setValue] and
     * [postValue], until owner be active, all recorded data will be notified to the [observer].
     *
     * @see observe
     */
    @MainThread
    open fun observeNoLoss(owner: LifecycleOwner, observer: Observer<in T>) {
        assertMainThread("observeNoLoss")
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            // ignore
            return
        }

        mObservers.eachObserverBox{
            if (observer === it.observer) {
                require(it is NoLossObserverWrapper) {
                    "Cannot add the same observer with different future"
                }
                require(it.isAttachedTo(owner)) {
                    "Cannot add the same observer with different lifecycles"
                }
                return
            }
        }

        val noLossValueObserver = AlwaysActiveToLifecycleBoundNoLossObserver(this, owner, observer)

        owner.lifecycle.addObserver(noLossValueObserver)

        observeForeverInner(noLossValueObserver)
    }

    /**
     * Using this function means that the [observer] no loss every value by calling [setValue]
     * and [postValue].
     *
     * You should manually call [removeObserver] to stop observing this [LiveEvent].
     *
     */
    @MainThread
    open fun observeForeverNoLoss(observer: Observer<in T>) {
        assertMainThread("observeForeverNoLoss")

        mObservers.eachObserverBox{
            if (observer === it.observer) {
                require(it is NoLossObserverWrapper) {
                    "Cannot add the same observer with different future"
                }
                require(it is AlwaysActive) {
                    "Cannot add the same observer with different lifecycles"
                }
                return
            }
        }

        val noLossValueObserver = AlwaysActiveNoLossObserver(observer)

        observeForeverInner(noLossValueObserver)
    }

    /**
     * Using this function means that the [observer] no loss and no sticky.
     *
     * @see observeNoLoss
     * @see observeNoSticky
     */
    @MainThread
    open fun observeNoStickyNoLoss(owner: LifecycleOwner, observer: Observer<in T>) {
        assertMainThread("observeNoStickyNoLoss")
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            // ignore
            return
        }

        mObservers.eachObserverBox {

            var currentObserver: Observer<*> = it
            while (currentObserver is ObserverBox<*>) {
                currentObserver = currentObserver.observer
            }
            if (currentObserver === observer) {
                require(it.observer is NoStickyObserverWrapper) {
                    "Cannot add the same observer with different future"
                }

                require(it.isAttachedTo(owner)) {
                    "Cannot add the same observer with different lifecycles"
                }

                require(it is AlwaysActiveToLifecycleBoundNoLossObserver) {
                    "Cannot add the same observer with different future"
                }
                return
            }

        }

        val shouldIgnoreSticky = mHasSet || (mVersion != START_VERSION).apply { mHasSet = this }
        val noStickyObserver = LifecycleBoundNoStickyObserverWrapper(this, owner, observer, shouldIgnoreSticky)
        owner.lifecycle.addObserver(noStickyObserver)
        val noLossValueObserver = AlwaysActiveToLifecycleBoundNoLossObserver(this, owner, noStickyObserver)
        owner.lifecycle.addObserver(noLossValueObserver)

        observeForeverInner(noLossValueObserver)
    }

    /**
     * Using this function means that the [observer] no loss and no sticky.
     * 
     * You should manually call [removeObserver] to stop observing this [LiveEvent].
     *
     * @see observeForeverNoSticky
     * @see observeForeverNoLoss
     */
    @MainThread
    open fun observeForeverNoStickyNoLoss(observer: Observer<in T>) {
        assertMainThread("observeForeverNoStickyNoLoss")

        mObservers.eachObserverBox {

            var currentObserver: Observer<*> = it
            while (currentObserver is ObserverBox<*>) {
                currentObserver = currentObserver.observer
            }
            if (currentObserver === observer) {
                require(it.observer is NoStickyObserverWrapper) {
                    "Cannot add the same observer with different future"
                }

                require(it is AlwaysActive) {
                    "Cannot add the same observer with different lifecycles"
                }

                require(it is AlwaysActiveNoLossObserver) {
                    "Cannot add the same observer with different future"
                }

                return
            }

        }

        val shouldIgnoreSticky = mHasSet || (mVersion != START_VERSION).apply { mHasSet = this }
        val noStickyObserver = AlwaysActiveNoStickyObserverWrapper(observer, shouldIgnoreSticky)
        val noLossValueObserver = AlwaysActiveNoLossObserver(noStickyObserver)
        observeForeverInner(noLossValueObserver)
    }

    @MainThread
    override fun removeObserver(observer: Observer<in T>) {
        assertMainThread("removeObserver")

        val finalObserver = mObservers.detachObserverBoxWith(observer) { detachObserverBox ->
            @Suppress("UNCHECKED_CAST")
            when (detachObserverBox) {
                is NoStickyObserverWrapper -> {
                    detachNoStickyObserver(detachObserverBox as NoStickyObserverWrapper<T>)
                }
                is NoLossObserverWrapper -> {
                    detachNoLossValueObserver(detachObserverBox as NoLossObserverWrapper<T>)
                }
            }
        }

        removeObserverInner(finalObserver)
    }

    @MainThread
    @Suppress("UNCHECKED_CAST")
    override fun removeObservers(owner: LifecycleOwner) {
        assertMainThread("removeObservers")
        mObservers.eachObserverBox {observerBox ->
            if (observerBox.isAttachedTo(owner) && observerBox.observer !is ObserverBox) {
                removeObserver(observerBox.observer)
            }
        }

        removeObserversInner(owner)
    }

    @MainThread
    protected override fun setValue(value: T) {
        assertMainThread("setValue")
        considerNotifyForPossibleLostPendingValue()
        setValueInner(value)
    }

    protected override fun postValue(value: T) {
        synchronized(mDataLock) {
            recodePossibleLostPendingDataIfExistPending()
            postValueInner(value)
        }
    }

    override fun getValue(): T? {
        return super.getValue()
    }

    private fun detachNoStickyObserver(observer: NoStickyObserverWrapper<T>) {
        observer.detachObserver()
    }

    private fun detachNoLossValueObserver(observer: NoLossObserverWrapper<T>) {
        observer.detachObserver()
        observer.activeStateChanged(false)
    }

    private fun setValueInner(value: T) {
        super.setValue(value)
    }

    private fun postValueInner(value: T) {
        super.postValue(value)
    }

    private fun observeInner(owner: LifecycleOwner, observer: Observer<in T> ) {
        super.observe(owner, observer)
    }

    private fun observeForeverInner(observer: Observer<in T>) {
        super.observeForever(observer)
    }

    private fun removeObserversInner(owner: LifecycleOwner) {
        super.removeObservers(owner)
    }

    private fun removeObserverInner(observer: Observer<in T>) {
        super.removeObserver(observer)
    }

    private fun considerNotifyForPossibleLostPendingValue() {
        val head: LostValue.Head<T>?
        synchronized(mDataLock) {
            head = this.mPossibleLostPendingValueHead ?: return
            mPossibleLostPendingValueHead = null
        }

        mObservers.eachObserverBox { observerBox ->
            if (observerBox is  NoLossObserverWrapper) {
                @Suppress("UNCHECKED_CAST")
                considerNotifyPossibleLostPendingValue(
                    observerBox as NoLossObserverWrapper<T>,
                    head!!
                )
            }
        }
    }

    private fun considerNotifyPossibleLostPendingValue(
        noLossObserverWrapper: NoLossObserverWrapper<T>,
        head: LostValue.Head<T>
    ) {
        noLossObserverWrapper.considerNotifyPossibleLostPendingValue(head)
    }

    private fun recodePossibleLostPendingDataIfExistPending() {
        @Suppress("UNCHECKED_CAST")
        val pendingData = mPendingData as T
        val exist = pendingData !== NOT_SET
        if (exist) {
            // record for already existed pending data, because the pending data is lost possible.
            recordPossibleLostPendingValue(pendingData)
        }
    }

    private fun recordPossibleLostPendingValue(pendingData: T) {
        var head = this.mPossibleLostPendingValueHead
        if (head == null) {
            head = LostValue.Head(pendingData)
            this.mPossibleLostPendingValueHead = head
        }else{
            head.appendLatest(pendingData)
        }
    }
}
