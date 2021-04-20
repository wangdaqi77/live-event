package androidx.lifecycle.wrapper

import android.os.Handler
import android.os.HandlerThread
import androidx.arch.core.internal.SafeIterableMap
import androidx.lifecycle.*
import androidx.lifecycle.wrapper.internal.*

open class LiveBackgroundEvent<T> : Observers<T> {

    companion object {
        private const val START_VERSION = -1
        private val NOT_SET = Any()
    }

    private val mDispatcher = BackgroundEventDispatcher()
    private val mDataLock = Any()
    @Volatile
    private var mData: Any? = null
    private val mObservers: SafeIterableMap<Observer<in T>, ObserverWrapper> = SafeIterableMap()
    private var mActiveCount = 0
    // when setData is called, we set the pending data and actual data swap happens on the
    // dispatchPip
    @Volatile
    private var mPendingData: Any? = NOT_SET
    private var mVersion: Int
    private var mDispatchingValue = false
    private var mDispatchInvalidated = false
    private val mPostValueRunnable = Runnable {
        var newValue: Any?
        synchronized(mDataLock) {
            newValue = mPendingData
            mPendingData = NOT_SET
        }
        @Suppress("UNCHECKED_CAST")
        setValue(newValue as T)
    }
    @Volatile
    private var mPossibleLostPendingValueHead : LostValue.Head<T>? = null

    /**
     * Creates a LiveBackgroundEvent initialized with the given `value`.
     *
     * @param value initial value
     */
    constructor(value: T) {
        mData = value
        mVersion = START_VERSION + 1
    }

    /**
     * Creates a LiveBackgroundEvent with no value assigned to it.
     */
    constructor() {
        mData = NOT_SET
        mVersion = START_VERSION
    }

    private fun considerNotify(observer: ObserverWrapper) {
        if (!observer.mActive) {
            return
        }
        // Check latest state b4 dispatch. Maybe it changed state but we didn't get the event yet.
        //
        // we still first check observer.active to keep it as the entrance for events. So even if
        // the observer moved to an active state, if we've not received that event, we better not
        // notify for a more predictable notification order.
        if (!observer.shouldBeActive()) {
            observer.activeStateChanged(false)
            return
        }
        if (observer.mLastVersion >= mVersion) {
            return
        }
        observer.mLastVersion = mVersion
        @Suppress("UNCHECKED_CAST")
        observer.mObserver.onChanged(mData as T)
    }

    private fun  dispatchingValue(initiator: ObserverWrapper?) {
        mDispatcher.dispatch {
            var initiator1 = initiator
            if (mDispatchingValue) {
                mDispatchInvalidated = true
                return@dispatch
            }
            mDispatchingValue = true
            do {
                mDispatchInvalidated = false
                if (initiator1 != null) {
                    considerNotify(initiator1)
                    initiator1 = null
                } else {
                    @Suppress("INACCESSIBLE_TYPE")
                    val iterator: Iterator<Map.Entry<Observer<in T>, ObserverWrapper>> =
                        mObservers.iteratorWithAdditions()
                    while (iterator.hasNext()) {
                        considerNotify(iterator.next().value)
                        if (mDispatchInvalidated) {
                            break
                        }
                    }
                }
            } while (mDispatchInvalidated)
            mDispatchingValue = false
        }
    }

    private fun considerNotifyForPossibleLostPendingValue() {
        val head: LostValue.Head<T>? = this.mPossibleLostPendingValueHead ?: return
        mPossibleLostPendingValueHead = null

        mObservers.eachObserverBox { observerBox ->
            if (observerBox is NoLossObserverWrapper) {
                @Suppress("UNCHECKED_CAST")
                considerNotifyPossibleLostPendingValue(observerBox as NoLossObserverWrapper<T>, head!!)
            }
        }
    }

    private fun considerNotifyPossibleLostPendingValue(noLossObserverWrapper: NoLossObserverWrapper<T>, head: LostValue.Head<T>){
        noLossObserverWrapper.considerNotifyPossibleLostPendingValue(head)
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

    /**
     * Adds the given observer to the observers list within the lifespan of the given
     * owner. The events are dispatched on the main thread. If LiveBackgroundEvent already
     * has data set, it will be delivered to the observer.
     *
     *
     * The observer will only receive events if the owner is in [Lifecycle.State.STARTED]
     * or [Lifecycle.State.RESUMED] state (active).
     *
     *
     * If the owner moves to the [Lifecycle.State.DESTROYED] state, the observer will
     * automatically be removed.
     *
     *
     * When data changes while the `owner` is not active, it will not receive any updates.
     * If it becomes active again, it will receive the last available data automatically.
     *
     *
     * LiveBackgroundEvent keeps a strong reference to the observer and the owner as long
     * as the given LifecycleOwner is not destroyed. When it is destroyed, LiveBackgroundEvent
     * removes references to the observer &amp; the owner.
     *
     *
     * If the given owner is already in [Lifecycle.State.DESTROYED] state, LiveBackgroundEvent
     * ignores the call.
     *
     *
     * If the given owner, observer tuple is already in the list, the call is ignored.
     * If the observer is already in the list with another owner, LiveBackgroundEvent throws an
     * [IllegalArgumentException].
     *
     * @param owner    The LifecycleOwner which controls the observer
     * @param observer The observer that will receive the events
     */
    open fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        mDispatcher.dispatch {

            if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                // ignore
                return@dispatch
            }
            val wrapper = LifecycleBoundObserver(owner, observer)
            val existing: ObserverWrapper? = mObservers.putIfAbsent(observer, wrapper)
            require(!(existing != null && !existing.isAttachedTo(owner))) {
                "Cannot add the same observer with different lifecycles"
            }
            if (existing != null) {
                return@dispatch
            }
            owner.lifecycle.addObserver(wrapper)

        }
    }

    /**
     * Adds the given observer to the observers list. This call is similar to [observe]
     * with a LifecycleOwner, which is always active. This means that the given observer
     * will receive all events and will never be automatically removed. You should manually
     * call [removeObserver] to stop observing this LiveBackgroundEvent.
     *
     * While LiveBackgroundEvent has one of such observers, it will be considered
     * as active.
     *
     * If the observer was already added with an owner to this LiveBackgroundEvent,
     * LiveBackgroundEvent throws an [IllegalArgumentException].
     *
     * @param observer The observer that will receive the events
     */
    open fun observeForever(observer: Observer<in T>) {
        mDispatcher.dispatch {
            val wrapper = AlwaysActiveObserver(observer)
            val existing = mObservers.putIfAbsent(observer, wrapper)

            if (existing != null) {
                return@dispatch
            }
            wrapper.activeStateChanged(true)
        }

    }

    /**
     * [observe] is a sticky function, this function is to shield the sticky.
     *
     * @see observe
     */
    open fun observeNoSticky(owner: LifecycleOwner, observer:Observer<in T>) {

        mDispatcher.dispatch {

            if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                // ignore
                return@dispatch
            }
            mObservers.eachObserverBox{
                if (observer === it.observer) {
                    require(it is NoStickyObserverWrapper) {
                        "Cannot add the same observer with different future"
                    }
                    require(it.isAttachedTo(owner)) {
                        "Cannot add the same observer with different lifecycles"
                    }
                    return@dispatch
                }
            }

            if(mVersion == START_VERSION) {
                val noStickyObserver = LifecycleBoundNoStickyObserverWrapper(this, owner, observer, false)
                owner.lifecycle.addObserver(noStickyObserver)
                observe(owner, noStickyObserver)
                return@dispatch
            }

            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                val noStickyObserver = LifecycleBoundNoStickyObserverWrapper(this, owner, observer, true)
                owner.lifecycle.addObserver(noStickyObserver)
                observe(owner, noStickyObserver)
            }else{
                val noStickyObserver = LifecycleBoundNoStickyObserverWrapper(this, owner, observer, false)
                owner.lifecycle.addObserver(noStickyObserver)
                observe(owner, noStickyObserver)
                @Suppress("INACCESSIBLE_TYPE")
                val value = mObservers.putIfAbsent(noStickyObserver, null)
                value.mLastVersion = mVersion
            }
        }
    }

    /**
     * [observeForever] is a sticky function, this function is to shield the sticky.
     *
     * You should manually call [removeObserver] to stop observing this [LiveBackgroundEvent].
     *
     * @see observeForever
     */
    open fun observeForeverNoSticky(observer: Observer<in T>) {
        mDispatcher.dispatch {
            mObservers.eachObserverBox{
                if (observer === it.observer) {
                    require(it is NoStickyObserverWrapper) {
                        "Cannot add the same observer with different future"
                    }
                    require(it is AlwaysActive) {
                        "Cannot add the same observer with different lifecycles"
                    }
                    return@dispatch
                }
            }

            if(mVersion == START_VERSION) {
                val noStickyObserver = AlwaysActiveNoStickyObserverWrapper(observer, false)
                observeForever(noStickyObserver)
                return@dispatch
            }

            val noStickyObserver = AlwaysActiveNoStickyObserverWrapper(observer, true)
            observeForever(noStickyObserver)
        }
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
    open fun observeNoLoss(owner: LifecycleOwner, observer: Observer<in T>) {
        mDispatcher.dispatch {
            if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                // ignore
                return@dispatch
            }

            mObservers.eachObserverBox{
                if (observer === it.observer) {
                    require(it is NoLossObserverWrapper) {
                        "Cannot add the same observer with different future"
                    }
                    require(it.isAttachedTo(owner)) {
                        "Cannot add the same observer with different lifecycles"
                    }
                    return@dispatch
                }
            }

            val noLossValueObserver = AlwaysActiveToLifecycleBoundNoLossObserver(this, owner, observer)

            owner.lifecycle.addObserver(noLossValueObserver)

            observeForever(noLossValueObserver)
        }
    }

    /**
     * Using this function means that the [observer] no loss every value by calling [setValue]
     * and [postValue].
     *
     * You should manually call [removeObserver] to stop observing this [LiveBackgroundEvent].
     *
     */
    open fun observeForeverNoLoss(observer: Observer<in T>) {
        mDispatcher.dispatch {
            mObservers.eachObserverBox{
                if (observer === it.observer) {
                    require(it is NoLossObserverWrapper) {
                        "Cannot add the same observer with different future"
                    }
                    require(it is AlwaysActive) {
                        "Cannot add the same observer with different lifecycles"
                    }
                    return@dispatch
                }
            }

            val noLossValueObserver = AlwaysActiveNoLossObserver(observer)

            observeForever(noLossValueObserver)
        }

    }

    /**
     * Using this function means that the [observer] no loss and no sticky.
     *
     * @see observeNoLoss
     * @see observeNoSticky
     */
    open fun observeNoStickyNoLoss(owner: LifecycleOwner, observer: Observer<in T>) {
        mDispatcher.dispatch {
            if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                // ignore
                return@dispatch
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
                    return@dispatch
                }

            }

            val shouldIgnoreSticky = mVersion != START_VERSION
            val noStickyObserver = LifecycleBoundNoStickyObserverWrapper(this, owner, observer, shouldIgnoreSticky)
            owner.lifecycle.addObserver(noStickyObserver)
            val noLossValueObserver = AlwaysActiveToLifecycleBoundNoLossObserver(this, owner, noStickyObserver)
            owner.lifecycle.addObserver(noLossValueObserver)

            observeForever(noLossValueObserver)
        }
    }

    /**
     * Using this function means that the [observer] no loss and no sticky.
     *
     * You should manually call [removeObserver] to stop observing this [LiveBackgroundEvent].
     *
     * @see observeForeverNoSticky
     * @see observeForeverNoLoss
     */
    open fun observeForeverNoStickyNoLoss(observer: Observer<in T>) {
        mDispatcher.dispatch {
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

                    return@dispatch
                }

            }

            val shouldIgnoreSticky = mVersion != START_VERSION
            val noStickyObserver = AlwaysActiveNoStickyObserverWrapper(observer, shouldIgnoreSticky)
            val noLossValueObserver = AlwaysActiveNoLossObserver(noStickyObserver)
            observeForever(noLossValueObserver)
        }

    }

    private fun detachNoStickyObserver(observer: NoStickyObserverWrapper<T>) {
        observer.detachObserver()
    }

    private fun detachNoLossValueObserver(observer: NoLossObserverWrapper<T>) {
        observer.detachObserver()
        observer.activeStateChanged(false)
    }

    /**
     * Removes all observers that are tied to the given [LifecycleOwner].
     *
     * @param owner The `LifecycleOwner` scope for the observers to be removed.
     */
    open fun removeObservers(owner: LifecycleOwner) {
        mDispatcher.dispatch {
            mObservers.eachObserverBox {observerBox ->
                if (observerBox.isAttachedTo(owner) && observerBox.observer !is ObserverBox) {
                    removeObserver(observerBox.observer)
                }
            }

            for ((key, value) in mObservers) {
                if (value.isAttachedTo(owner)) {
                    removeObserver(key)
                }
            }
        }

    }

    /**
     * Removes the given observer from the observers list.
     *
     * @param observer The Observer to receive events.
     */
    override fun removeObserver(observer: Observer<in T>) {
        mDispatcher.dispatch {
            val finalObserver = mObservers.detachObserverBoxWith(observer){ detachObserverBox ->
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

            val removed = mObservers.remove(finalObserver) ?: return@dispatch
            removed.detachObserver()
            removed.activeStateChanged(false)
        }
    }

    open fun hasActiveObservers(): Boolean {
        return mActiveCount > 0
    }

    open fun hasObservers(): Boolean {
        return mObservers.size() > 0
    }

    /**
     * Posts a task to background thread to set the given value. So if you have a following code
     * executed in same thread:
     *
     * liveBackgroundEvent.postValue("a");
     * liveBackgroundEvent.postValue("b");
     *
     * The value "b" would be set at first and later the background thread would override it with
     * the value "a".
     *
     * @param value The new value
     */
    protected open fun postValue(value: T) {
        var postTask: Boolean
        synchronized(mDataLock) {
            postTask = mPendingData === NOT_SET
            if (!postTask) {
                // record for already existed pending data, because the pending data is lost possible.
                @Suppress("UNCHECKED_CAST")
                recordPossibleLostPendingValue(mPendingData as T)
            }
            mPendingData = value
        }
        if (!postTask) {
            return
        }
        mDispatcher.dispatch(mPostValueRunnable)
    }

    /**
     * Sets the value. If there are active observers, the value will be dispatched to them
     * in background thread.
     *
     * @param value The new value
     */
    protected open fun setValue(value: T) {
        mDispatcher.dispatch {
            considerNotifyForPossibleLostPendingValue()
            mVersion++
            mData = value
            dispatchingValue(null)
        }
    }

    @Suppress("UNCHECKED_CAST")
    open fun getValue(): T? {
        val data = mData
        if (data !== NOT_SET) {
            return data as T
        }
        return null
    }

    open fun onActive() { }

    open fun onInactive() { }

    private abstract inner class ObserverWrapper internal constructor(val mObserver: Observer<in T>) {
        var mActive = false
        var mLastVersion = START_VERSION
        abstract fun shouldBeActive(): Boolean
        open fun isAttachedTo(owner: LifecycleOwner): Boolean {
            return false
        }

        open fun detachObserver() {}
        fun activeStateChanged(newActive: Boolean) {
            if (newActive == mActive) {
                return
            }
            // immediately set active state, so we'd never dispatch anything to inactive
            // owner
            mActive = newActive
            val wasInactive = mActiveCount == 0
            mActiveCount += if (mActive) 1 else -1
            if (wasInactive && mActive) {
                onActive()
            }
            if (mActiveCount == 0 && !mActive) {
                onInactive()
            }
            if (mActive) {
                dispatchingValue(this)
            }
        }

    }

    private inner class AlwaysActiveObserver internal constructor(observer: Observer<in T>?) :
        ObserverWrapper(observer!!) {
        override fun shouldBeActive(): Boolean {
            return true
        }
    }

    private inner class LifecycleBoundObserver
    constructor(private val mOwner: LifecycleOwner, observer: Observer<in T>)
        : ObserverWrapper(observer), LifecycleEventObserver {

        override fun shouldBeActive(): Boolean {
            return mOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            mDispatcher.dispatch {
                var currentState = mOwner.lifecycle.currentState
                if (currentState == Lifecycle.State.DESTROYED) {
                    removeObserver(mObserver)
                    return@dispatch
                }
                var prevState: Lifecycle.State? = null
                while (prevState != currentState) {
                    prevState = currentState

                    activeStateChanged(shouldBeActive())
                    currentState = mOwner.lifecycle.currentState
                }
            }

        }

        override fun isAttachedTo(owner: LifecycleOwner): Boolean {
            return mOwner === owner
        }

        override fun detachObserver() {
            mOwner.lifecycle.removeObserver(this)
        }

    }

    private class BackgroundEventDispatcher {
        private val dispatchPipe = HandlerThread("LiveBackgroundEventDispatcher${hashCode()}-thread")
        private var dispatcher: Handler
        private val inDispatchPipe
            get() = Thread.currentThread() === dispatchPipe

        init {
            dispatchPipe.start()
            dispatcher = Handler(dispatchPipe.looper)
        }

        fun dispatch(runnable: Runnable) {
            if (!inDispatchPipe) {
                dispatcher.post(runnable)
                return
            }
            runnable.run()
        }

        fun dispatch(block: () -> Unit) {
            if (!inDispatchPipe) {
                dispatcher.post {block()}
                return
            }
            block()
        }
    }

}