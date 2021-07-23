package wang.lifecycle

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.annotation.MainThread
import androidx.lifecycle.*
import wang.lifecycle.internal.*

/**
 * LiveEvent is a event holder class for observe event on main thread, that
 * can be observed within a given lifecycle.
 *
 * If you need observe event on background thread, you can use [BackgroundLiveEvent].
 *
 * @param T The type of data hold by this instance.
 */
open class LiveEvent<T> : InternalSupportedLiveData<T> {

    constructor() : super()

    constructor(value: T) : super(value)

    @Volatile
    private var mLostPendingValueHead: LostValue.Head<T>? = null
    private var noLossObserverCount = 0
    private val hasNoLossObserver
        get() = noLossObserverCount > 0
    private val mPostValueRunnable = Runnable {
        var newValue: Any?
        synchronized(mDataLock) {
            newValue = mPendingData
            mPendingData = NOT_SET
            notifyLostPendingValue()
        }
        @Suppress("UNCHECKED_CAST")
        setValue(newValue as T)
    }

    private fun observeInner(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner, observer)
    }

    private fun observeForeverInner(observer: Observer<in T>) {
        if (observer is NoLossObserverBox) noLossObserverCount++
        super.observeForever(observer)
    }

    private fun onChangedForObserver(observer: Observer<in T>, value: T) {
        observer.onChanged(value)
    }

    private fun onLifecycleOwnerDestroyForObserverBox(observer: Observer<in T>) {
        removeObserver(observer)
    }

    private fun detachNoStickyObserver(observer: NoStickyObserverBox<T>) {
        observer.detachObserver()
    }

    private fun detachNoLossValueObserver(observer: NoLossObserverBox<T>) {
        noLossObserverCount--
        observer.detachObserver()
        observer.activeStateChanged(false)
    }

    private fun recordLostPendingValue(pendingData: T) {
        var head = this.mLostPendingValueHead
        if (head == null) {
            head = LostValue.Head(pendingData)
            this.mLostPendingValueHead = head
        }else{
            head.appendTail(pendingData)
        }
    }

    private fun notifyLostPendingValue() {
        val head: LostValue.Head<T> = mLostPendingValueHead?:return
        mLostPendingValueHead = null

        mObservers.eachObserverBox { observerBox ->
            if (observerBox is NoLossObserverBox) {
                head.eachToTail(block = observerBox::onChanged)
            }
        }
    }

    private fun considerNotifyForNoLossObserver() {
        @Suppress("INACCESSIBLE_TYPE")
        val iterator = mObservers.iteratorWithAdditions() as Iterator<Map.Entry<Observer<in T>, *>>

        while (iterator.hasNext()) {
            val entry = iterator.next()
            val observerWrapper = entry.value!!
            var observer = entry.key
            while (observer is ObserverBox) {
                if (observer is NoLossObserverBox) {
                    InternalReflect.considerNotify(this, observerWrapper)
                    break
                }
                observer = observer.observer
            }

        }
    }

    private fun checkObserver(observer: Observer<*>){
        if (observer is BackgroundObserver) {
            Log.w("live-event", "BackgroundObserver is not supported in LiveEvent, you maybe want to use BackgroundLiveEvent.")
        }
    }

    /**
     * @see LiveData.observe
     */
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        checkObserver(observer)
        observeInner(owner, observer)
    }

    /**
     * @see LiveData.observeForever
     */
    override fun observeForever(observer: Observer<in T>) {
        checkObserver(observer)
        observeForeverInner(observer)
    }

    /**
     * [observe] is a sticky function, this function is to shield the sticky.
     *
     * @see observe
     */
    @MainThread
    open fun observeNoSticky(owner: LifecycleOwner, observer: Observer<in T>) {
        checkObserver(observer)
        assertMainThread("observeNoSticky")
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            // ignore
            return
        }

        mObservers.eachObserverBox{
            if (observer === it.observer) {
                require(it is NoStickyObserverBox) {
                    "Cannot add the same observer with different future"
                }
                require(it.isAttachedTo(owner)) {
                    "Cannot add the same observer with different lifecycles"
                }
                return
            }
        }

        if(version == START_VERSION) {
            val noStickyObserver = LifecycleBoundNoStickyObserverBox(owner, observer, false, ::onChangedForObserver, ::onLifecycleOwnerDestroyForObserverBox)
            owner.lifecycle.addObserver(noStickyObserver)
            observeInner(owner, noStickyObserver)
            return
        }

        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            val noStickyObserver = LifecycleBoundNoStickyObserverBox(owner, observer, true, ::onChangedForObserver, ::onLifecycleOwnerDestroyForObserverBox)
            owner.lifecycle.addObserver(noStickyObserver)
            observeInner(owner, noStickyObserver)
        }else{
            val noStickyObserver = LifecycleBoundNoStickyObserverBox(owner, observer, false, ::onChangedForObserver, ::onLifecycleOwnerDestroyForObserverBox)
            owner.lifecycle.addObserver(noStickyObserver)
            observeInner(owner, noStickyObserver)
            syncVersion(noStickyObserver)
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
        checkObserver(observer)
        assertMainThread("observeForeverNoSticky")

        mObservers.eachObserverBox{
            if (observer === it.observer) {
                require(it is NoStickyObserverBox) {
                    "Cannot add the same observer with different future"
                }
                require(it is AlwaysActive) {
                    "Cannot add the same observer with different lifecycles"
                }
                return
            }
        }

        if(version == START_VERSION) {
            val noStickyObserver = AlwaysActiveNoStickyObserverBox(observer, false, ::onChangedForObserver)
            observeForeverInner(noStickyObserver)
            return
        }

        val noStickyObserver = AlwaysActiveNoStickyObserverBox(observer, true, ::onChangedForObserver)

        observeForeverInner(noStickyObserver)
    }

    /**
     * Using this function means calling [setValue] and [postValue] that no loss every event
     * for the [observer].
     *
     * If the [owner] be active, will record pending data when call [postValue], until next time
     * [setValue] is called, all recorded data will be notified to the [observer].
     *
     * If the [owner] be inactive, will record data and pending data when call [setValue] and
     * [postValue], until owner be active, all recorded data will be notified to the [observer].
     *
     * @see observe
     */
    @MainThread
    open fun observeNoLoss(owner: LifecycleOwner, observer: Observer<in T>) {
        checkObserver(observer)
        assertMainThread("observeNoLoss")
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            // ignore
            return
        }

        mObservers.eachObserverBox{
            if (observer === it.observer) {
                require(it is NoLossObserverBox) {
                    "Cannot add the same observer with different future"
                }
                require(it.isAttachedTo(owner)) {
                    "Cannot add the same observer with different lifecycles"
                }
                return
            }
        }

        val noLossValueObserver = AlwaysActiveToLifecycleBoundNoLossObserverBox(owner, observer, ::onChangedForObserver, ::onLifecycleOwnerDestroyForObserverBox)

        owner.lifecycle.addObserver(noLossValueObserver)

        observeForeverInner(noLossValueObserver)
    }

    /**
     * Using this function means calling [setValue] and [postValue] that no loss every event
     * for the [observer].
     *
     * You should manually call [removeObserver] to stop observing this [LiveEvent].
     */
    @MainThread
    open fun observeForeverNoLoss(observer: Observer<in T>) {
        checkObserver(observer)
        assertMainThread("observeForeverNoLoss")

        mObservers.eachObserverBox{
            if (observer === it.observer) {
                require(it is NoLossObserverBox) {
                    "Cannot add the same observer with different future"
                }
                require(it is AlwaysActive) {
                    "Cannot add the same observer with different lifecycles"
                }
                return
            }
        }

        val noLossValueObserver = AlwaysActiveNoLossObserverBox(observer, ::onChangedForObserver)

        observeForeverInner(noLossValueObserver)
    }

    /**
     * Using this function means no loss and no sticky for the [observer].
     *
     * @see observeNoLoss
     * @see observeNoSticky
     */
    @MainThread
    open fun observeNoStickyNoLoss(owner: LifecycleOwner, observer: Observer<in T>) {
        checkObserver(observer)
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
                require(it.observer is NoStickyObserverBox) {
                    "Cannot add the same observer with different future"
                }

                require(it.isAttachedTo(owner)) {
                    "Cannot add the same observer with different lifecycles"
                }

                require(it is AlwaysActiveToLifecycleBoundNoLossObserverBox) {
                    "Cannot add the same observer with different future"
                }
                return
            }

        }

        val shouldIgnoreSticky =  version != START_VERSION
        val noStickyObserver = LifecycleBoundNoStickyObserverBox(owner, observer, shouldIgnoreSticky, ::onChangedForObserver, ::onLifecycleOwnerDestroyForObserverBox)
        owner.lifecycle.addObserver(noStickyObserver)
        val noLossValueObserver = AlwaysActiveToLifecycleBoundNoLossObserverBox(owner, noStickyObserver, ::onChangedForObserver, ::onLifecycleOwnerDestroyForObserverBox)
        owner.lifecycle.addObserver(noLossValueObserver)

        observeForeverInner(noLossValueObserver)
    }

    /**
     * Using this function means no loss and no sticky for the [observer].
     * 
     * You should manually call [removeObserver] to stop observing this [LiveEvent].
     *
     * @see observeForeverNoSticky
     * @see observeForeverNoLoss
     */
    @MainThread
    open fun observeForeverNoStickyNoLoss(observer: Observer<in T>) {
        checkObserver(observer)
        assertMainThread("observeForeverNoStickyNoLoss")

        mObservers.eachObserverBox {

            var currentObserver: Observer<*> = it
            while (currentObserver is ObserverBox<*>) {
                currentObserver = currentObserver.observer
            }
            if (currentObserver === observer) {
                require(it.observer is NoStickyObserverBox) {
                    "Cannot add the same observer with different future"
                }

                require(it is AlwaysActive) {
                    "Cannot add the same observer with different lifecycles"
                }

                require(it is AlwaysActiveNoLossObserverBox) {
                    "Cannot add the same observer with different future"
                }

                return
            }

        }

        val shouldIgnoreSticky = version != START_VERSION
        val noStickyObserver = AlwaysActiveNoStickyObserverBox(observer, shouldIgnoreSticky, ::onChangedForObserver)
        val noLossValueObserver = AlwaysActiveNoLossObserverBox(noStickyObserver, ::onChangedForObserver)
        observeForeverInner(noLossValueObserver)
    }

    @MainThread
    override fun removeObserver(observer: Observer<in T>) {
        assertMainThread("removeObserver")

        val finalObserver = mObservers.detachObserverBoxWith(observer) { detachObserverBox ->
            @Suppress("UNCHECKED_CAST")
            when (detachObserverBox) {
                is NoStickyObserverBox -> {
                    detachNoStickyObserver(detachObserverBox as NoStickyObserverBox<T>)
                }
                is NoLossObserverBox -> {
                    detachNoLossValueObserver(detachObserverBox as NoLossObserverBox<T>)
                }
            }
        }

        super.removeObserver(finalObserver)
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

        super.removeObservers(owner)
    }

    @MainThread
    override fun setValue(value: T) {
        assertMainThread("setValue")

        // dispatchingValue to noLoss
        if (hasNoLossObserver && mDispatchingValue) {
            considerNotifyForNoLossObserver()
        }

        super.setValue(value)
    }

    override fun postValue(value: T) {
        var postTask: Boolean
        synchronized(mDataLock) {
            postTask = mPendingData === NOT_SET
            if (!postTask) {
                // record last pending data, because it will lost.
                @Suppress("UNCHECKED_CAST")
                recordLostPendingValue(mPendingData as T)
            }

            mPendingData = value
        }
        if (!postTask) {
            return
        }

        InternalMainExecutor.execute(mPostValueRunnable)
    }

    override fun getValue(): T? {
        return super.getValue()
    }
}
