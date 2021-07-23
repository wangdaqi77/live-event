package wang.lifecycle

import androidx.arch.core.internal.SafeIterableMap
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import wang.lifecycle.internal.*
import java.util.*

/**
 * BackgroundLiveEvent is a event holder class for observe event on background thread, that
 * can be observed within a given lifecycle.
 *
 * Use this [BackgroundObserver] if you want use the specified thread to receive events.
 * Example:
 * ```
 * // 1. custom a EventDispatcher
 * class CustomEventDispatcher : EventDispatcher {
 *      // custom a thread executor
 *      private val threadExecutor = Executors.newSingleThreadExecutor()
 *      override fun dispatch(runnable: Runnable) {
 *          // will notify a event by the threadExecutor
 *          threadExecutor.execute(runnable)
 *      }
 * }
 *
 * // 2. use BackgroundObserver to observe
 * backgroundLiveEvent.observe(owner, object : BackgroundObserver<Int>(customEventDispatcher){
 *      override fun onChanged(event: T) {
 *          // receive event in specified thread of the customEventDispatcher.
 *      }
 * }
 * ```
 *
 * Note custom dispatcher of BackgroundObserver takes precedence over global dispatcher.
 *
 * If you need observe event on main thread, you can use [LiveEvent].
 *
 * @param T The type of data hold by this instance.
 */
open class BackgroundLiveEvent<T> {

    companion object {
        private const val START_VERSION = InternalSupportedLiveData.START_VERSION
        private val NOT_SET = InternalSupportedLiveData.NOT_SET
        private val sDefaultScheduler = InternalDispatcher("default-dispatcher")
        private val sBackgroundLifecycleRegistry = WeakHashMap<LifecycleOwner, LifecycleRegistry>()
        internal val LifecycleOwner.backgroundLifecycle : Lifecycle
            get() = sBackgroundLifecycleRegistry[this] ?: throw RuntimeException("UnKnow!")

        private fun checkAndAttachBackgroundLifecycle(owner: LifecycleOwner) {
            if (sBackgroundLifecycleRegistry[owner] == null) {
                synchronized(this) {
                    if (sBackgroundLifecycleRegistry[owner] == null) {
                        sBackgroundLifecycleRegistry[owner] = LifecycleRegistry(owner).apply {
                            // LifecycleRegistry - main -> sDefaultScheduler
                            InternalReflect.closeCheckManiThreadOfLifecycleRegistry(this)
                            val runnable = Runnable {
                                val lifecycleEventObserver = object : LifecycleEventObserver {
                                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                                        if (event == Lifecycle.Event.ON_DESTROY) {
                                            owner.lifecycle.removeObserver(this)
                                        }
                                        sDefaultScheduler.dispatch{
                                            currentState = event.targetState
                                        }
                                    }

                                }
                                owner.lifecycle.addObserver(lifecycleEventObserver)
                            }

                            if (InternalMainExecutor.isMainThread) {
                                runnable.run()
                            }else {
                                InternalMainExecutor.execute(runnable)
                            }
                        }
                    }
                }
            }
        }
    }

    private val mPendingDataLock = Any()
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
    @Volatile
    private var mLostPendingValueHead : LostValue.Head<T>? = null
    private val mThreadValueMap by lazy { ThreadValueMap() }
    private var noLossObserverCount = 0
    private val hasNoLossObserver
        get() = noLossObserverCount > 0
    private val mPostValueRunnable = object : InternalRunnable {
        override fun run() {
            var newValue: Any?
            synchronized(mPendingDataLock) {
                newValue = mPendingData
                mPendingData = NOT_SET
                notifyLostPendingValue()
            }
            @Suppress("UNCHECKED_CAST")
            setValue(newValue as T)
        }
    }

    /**
     * Creates a BackgroundLiveEvent initialized with the given [value].
     *
     * @property value initial value.
     */
    constructor(value: T) {
        mData = value
        mVersion = START_VERSION + 1
    }

    /**
     * Creates a BackgroundLiveEvent with no value assigned to it.
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
        val data = mData as T
        onChangedForObserver(observer.mObserver, data)
    }

    private fun dispatchingValue(initiator: ObserverWrapper?) {
        if (mDispatchingValue) {
            mDispatchInvalidated = true
            return
        }
        mDispatchingValue = true

        var initiator1 = initiator
        do {
            mDispatchInvalidated = false
            if (initiator1 != null) {
                considerNotify(initiator1)
                initiator1 = null
            } else {
                @Suppress("INACCESSIBLE_TYPE")
                val iterator: Iterator<Map.Entry<Observer<in T>, ObserverWrapper>> = mObservers.iteratorWithAdditions()
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

    private fun onChangedForObserver(observer: Observer<in T>, value: T) {
        if (observer is BackgroundObserver) {
            val dispatcher = observer.dispatcher
            dispatcher.dispatch(object : InternalRunnable {
                override fun run() {
                    observer.onChanged(value)
                }
            })
        } else {
            sDefaultScheduler.dispatch {
                observer.onChanged(value)
            }
        }
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
        val head: LostValue.Head<T> = this.mLostPendingValueHead ?: return
        mLostPendingValueHead = null

        mObservers.eachObserverBox { observerBox ->
            if (observerBox is NoLossObserverBox) {
                head.eachToTail(block = observerBox::onChanged)
            }
        }
    }

    private fun considerNotifyForNoLossObserver() {
        @Suppress("INACCESSIBLE_TYPE")
        val iterator = mObservers.iteratorWithAdditions() as Iterator<Map.Entry<Observer<in T>, ObserverWrapper>>

        while (iterator.hasNext()) {
            val entry = iterator.next()
            val observerWrapper = entry.value
            var observer = entry.key
            while (observer is ObserverBox) {
                if (observer is NoLossObserverBox) {
                    considerNotify(observerWrapper)
                    break
                }
                observer = observer.observer
            }

        }
    }

    /**
     * Adds the given observer to the observers list within the lifespan of the given owner.
     * The events are dispatched on background thread. If BackgroundLiveEvent already has
     * data set, it will be delivered to the observer.
     *
     * The observer will only receive events if the owner is in [Lifecycle.State.STARTED]
     * or [Lifecycle.State.RESUMED] state (active).
     *
     * If the owner moves to the [Lifecycle.State.DESTROYED] state, the observer will
     * automatically be removed.
     *
     * When data changes while the [owner] is not active, it will not receive any updates.
     * If it becomes active again, it will receive the last available data automatically.
     *
     * BackgroundLiveEvent keeps a strong reference to the observer and the owner as long
     * as the given LifecycleOwner is not destroyed. When it is destroyed, BackgroundLiveEvent
     * removes references to the observer & the [owner].
     *
     * If the given owner is already in [Lifecycle.State.DESTROYED] state, BackgroundLiveEvent
     * ignores the call.
     *
     * If the given owner, observer tuple is already in the list, the call is ignored.
     * If the observer is already in the list with another owner, BackgroundLiveEvent throws an
     * [IllegalArgumentException].
     *
     * @param owner    The LifecycleOwner which controls the observer
     * @param observer The observer that will receive the events
     */
    open fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        observeInner(owner, observer)
    }

    private fun observeInner(
        owner: LifecycleOwner, observer: Observer<in T>,
        onAfter: ((LifecycleBoundObserver) -> Unit)? = null
    ) {
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            // ignore
            return
        }

        checkAndAttachBackgroundLifecycle(owner)

        sDefaultScheduler.dispatch {
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
            owner.backgroundLifecycle.addObserver(wrapper)
            onAfter?.invoke(wrapper)
        }
    }

    /**
     * Adds the given observer to the observers list. This call is similar to [observe]
     * with a LifecycleOwner, which is always active. This means that the given observer
     * will receive calling [setValue] all events and will never be automatically removed.
     * You should manually call [removeObserver] to stop observing this BackgroundLiveEvent.
     *
     * While BackgroundLiveEvent has one of such observers, it will be considered
     * as active.
     *
     * If the observer was already added with an owner to this BackgroundLiveEvent,
     * BackgroundLiveEvent throws an [IllegalArgumentException].
     *
     * @param observer The observer that will receive the events
     */
    open fun observeForever(observer: Observer<in T>) {
        observeForeverInner(observer)
    }

    private fun observeForeverInner(observer: Observer<in T>) {
        sDefaultScheduler.dispatch {
            noLossObserverCount++
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
    open fun observeNoSticky(owner: LifecycleOwner, observer: Observer<in T>) {

        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            // ignore
            return
        }

        checkAndAttachBackgroundLifecycle(owner)

        sDefaultScheduler.dispatch {
            if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                // ignore
                return@dispatch
            }

            mObservers.eachObserverBox{
                if (observer === it.observer) {
                    require(it is NoStickyObserverBox) {
                        "Cannot add the same observer with different future"
                    }
                    require(it.isAttachedTo(owner)) {
                        "Cannot add the same observer with different lifecycles"
                    }
                    return@dispatch
                }
            }

            if(mVersion == START_VERSION) {
                val noStickyObserver = LifecycleBoundNoStickyObserverBox(owner, observer, false, ::onChangedForObserver, ::onLifecycleOwnerDestroyForObserverBox, isBackground = true)

                owner.backgroundLifecycle.addObserver(noStickyObserver)
                observeInner(owner, noStickyObserver)
                return@dispatch
            }

            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                val noStickyObserver = LifecycleBoundNoStickyObserverBox(owner, observer, true, ::onChangedForObserver, ::onLifecycleOwnerDestroyForObserverBox, isBackground = true)
                owner.backgroundLifecycle.addObserver(noStickyObserver)
                observeInner(owner, noStickyObserver)
            }else{
                val noStickyObserver = LifecycleBoundNoStickyObserverBox(owner, observer, false, ::onChangedForObserver, ::onLifecycleOwnerDestroyForObserverBox, isBackground = true)
                owner.backgroundLifecycle.addObserver(noStickyObserver)
                observeInner(owner, noStickyObserver) {
                    it.mLastVersion = mVersion
                }
            }
        }
    }

    /**
     * [observeForever] is a sticky function, this function is to shield the sticky.
     *
     *
     * You should manually call [removeObserver] to stop observing this [BackgroundLiveEvent].
     *
     * @see observeForever
     */
    open fun observeForeverNoSticky(observer: Observer<in T>) {

        sDefaultScheduler.dispatch {
            mObservers.eachObserverBox {
                if (observer === it.observer) {
                    require(it is NoStickyObserverBox) {
                        "Cannot add the same observer with different future"
                    }
                    require(it is AlwaysActive) {
                        "Cannot add the same observer with different lifecycles"
                    }
                    return@dispatch
                }
            }
            if (mVersion == START_VERSION) {
                val noStickyObserver = AlwaysActiveNoStickyObserverBox(observer, false, ::onChangedForObserver)
                observeForeverInner(noStickyObserver)
                return@dispatch
            }

            val noStickyObserver = AlwaysActiveNoStickyObserverBox(observer, true, ::onChangedForObserver)
            observeForeverInner(noStickyObserver)
        }

    }

    /**
     * Using this function means calling [setValue] and [postValue] that no loss every event
     * for the [observer].
     *
     * * If the [owner] be active, will record pending data when call [postValue], until next time
     * [setValue] is called, all recorded data will be notified to the [observer].
     *
     * * If the [owner] be inactive, will record data and pending data when call [setValue] and
     * [postValue], until owner be active, all recorded data will be notified to the [observer].
     *
     * @see observe
     */
    open fun observeNoLoss(owner: LifecycleOwner, observer: Observer<in T>) {
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            // ignore
            return
        }

        checkAndAttachBackgroundLifecycle(owner)

        sDefaultScheduler.dispatch {
            if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                // ignore
                return@dispatch
            }

            mObservers.eachObserverBox {
                if (observer === it.observer) {
                    require(it is NoLossObserverBox) {
                        "Cannot add the same observer with different future"
                    }
                    require(it.isAttachedTo(owner)) {
                        "Cannot add the same observer with different lifecycles"
                    }
                    return@dispatch
                }
            }

            val noLossValueObserver = AlwaysActiveToLifecycleBoundNoLossObserverBox(owner, observer, ::onChangedForObserver, ::onLifecycleOwnerDestroyForObserverBox, isBackground = true)
            owner.backgroundLifecycle.addObserver(noLossValueObserver)
            observeForeverInner(noLossValueObserver)
        }

    }

    /**
     * Using this function means calling [setValue] and [postValue] that no loss every event
     * for the [observer].
     *
     * You should manually call [removeObserver] to stop observing this [BackgroundLiveEvent].
     *
     */
    open fun observeForeverNoLoss(observer: Observer<in T>) {
        sDefaultScheduler.dispatch {
            mObservers.eachObserverBox {
                if (observer === it.observer) {
                    require(it is NoLossObserverBox) {
                        "Cannot add the same observer with different future"
                    }
                    require(it is AlwaysActive) {
                        "Cannot add the same observer with different lifecycles"
                    }
                    return@dispatch
                }
            }

            val noLossValueObserver = AlwaysActiveNoLossObserverBox(observer, ::onChangedForObserver)
            observeForeverInner(noLossValueObserver)
        }
    }

    /**
     * Using this function means no loss and no sticky for the [observer].
     *
     * @see observeNoLoss
     * @see observeNoSticky
     */
    open fun observeNoStickyNoLoss(owner: LifecycleOwner, observer: Observer<in T>) {
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            // ignore
            return
        }

        checkAndAttachBackgroundLifecycle(owner)

        sDefaultScheduler.dispatch {
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
                    require(it.observer is NoStickyObserverBox) {
                        "Cannot add the same observer with different future"
                    }

                    require(it.isAttachedTo(owner)) {
                        "Cannot add the same observer with different lifecycles"
                    }

                    require(it is AlwaysActiveToLifecycleBoundNoLossObserverBox) {
                        "Cannot add the same observer with different future"
                    }
                    return@dispatch
                }

            }

            val shouldIgnoreSticky = mVersion != START_VERSION
            val noStickyObserver =
                LifecycleBoundNoStickyObserverBox(owner, observer, shouldIgnoreSticky, ::onChangedForObserver, ::onLifecycleOwnerDestroyForObserverBox, isBackground = true)
            owner.backgroundLifecycle.addObserver(noStickyObserver)
            val noLossValueObserver =
                AlwaysActiveToLifecycleBoundNoLossObserverBox(owner, noStickyObserver, ::onChangedForObserver, ::onLifecycleOwnerDestroyForObserverBox, isBackground = true)
            owner.backgroundLifecycle.addObserver(noLossValueObserver)
            observeForeverInner(noLossValueObserver)
        }
    }

    /**
     * Using this function means no loss and no sticky for the [observer].
     *
     * You should manually call [removeObserver] to stop observing this [BackgroundLiveEvent].
     *
     * @see observeForeverNoSticky
     * @see observeForeverNoLoss
     */
    open fun observeForeverNoStickyNoLoss(observer: Observer<in T>) {
        sDefaultScheduler.dispatch {
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

                    return@dispatch
                }

            }

            val shouldIgnoreSticky = mVersion != START_VERSION
            val noStickyObserver = AlwaysActiveNoStickyObserverBox(observer, shouldIgnoreSticky, ::onChangedForObserver)
            val noLossValueObserver = AlwaysActiveNoLossObserverBox(noStickyObserver, ::onChangedForObserver)
            observeForeverInner(noLossValueObserver)

        }

    }

    /**
     * Removes all observers that are tied to the given [LifecycleOwner].
     *
     * @param owner The [LifecycleOwner] scope for the observers to be removed.
     */
    open fun removeObservers(owner: LifecycleOwner) {
        sDefaultScheduler.dispatch {
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
    open fun removeObserver(observer: Observer<in T>) {
        sDefaultScheduler.dispatch {
            val finalObserver = mObservers.detachObserverBoxWith(observer){ detachObserverBox ->
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
            val removed = mObservers.remove(finalObserver) ?: return@dispatch
            removed.detachObserver()
            removed.activeStateChanged(false)
        }
    }

    /**
     * Returns true if this BackgroundLiveEvent has active observers.
     * Note that calling this method does not guarantee that the latest value set will be received.
     *
     * @return true if this BackgroundLiveEvent has active observers
     */
    open fun hasActiveObservers(): Boolean {
        return mActiveCount > 0
    }

    /**
     * Returns true if this BackgroundLiveEvent has observers.
     * Note that calling this method does not guarantee that the latest value set will be received.
     *
     * @return true if this BackgroundLiveEvent has observers
     */
    open fun hasObservers(): Boolean {
        return mObservers.size() > 0
    }

    /**
     * Posts a task to background thread to set the given value. So if you have a following code
     * executed in same thread:
     *
     * backgroundLiveEvent.postValue("a");
     * backgroundLiveEvent.postValue("b");
     *
     * The value "b" would be set at first and later the background thread would override it with
     * the value "a".
     *
     * @param value The new value
     */
    protected open fun postValue(value: T) {
        var postTask: Boolean
        synchronized(mPendingDataLock) {
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
        sDefaultScheduler.dispatch(mPostValueRunnable)
    }

    /**
     * Sets the value. If there are active observers, the value will be dispatched to them
     * in background thread.
     *
     * @param value The new value
     */
    protected open fun setValue(value: T) {
        val threadValue = mThreadValueMap.setValueAndGet(value)
        sDefaultScheduler.dispatch {
            // dispatchingValue to noLoss
            if (hasNoLossObserver && mDispatchingValue) {
                considerNotifyForNoLossObserver()
            }

            mVersion++
            mData = value
            threadValue.remove()
            dispatchingValue(null)
        }
    }

    /**
     * Returns the current value.
     * Note that calling this method does not guarantee that the latest value set will be received.
     *
     * @return the current value
     */
    open fun getValue(): T? {
        val threadValue = mThreadValueMap.get()
        if (threadValue != null) {
            return threadValue.value
        }

        val data = mData
        if (data !== NOT_SET) {
            @Suppress("UNCHECKED_CAST")
            return data as T
        }
        return null
    }

    protected open fun onActive() { }

    protected open fun onInactive() { }

    private abstract inner class ObserverWrapper internal constructor(val mObserver: Observer<in T>) {
        var mActive = false
        var mLastVersion = START_VERSION

        abstract fun shouldBeActive(): Boolean

        open fun isAttachedTo(owner: LifecycleOwner): Boolean {
            return false
        }

        open fun detachObserver() { }

        fun activeStateChanged(newActive: Boolean) {
            if (newActive == mActive) {
                return
            }
            // immediately set active state, so we not dispatch anything to inactive owner in here
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

    private inner class AlwaysActiveObserver internal constructor(observer: Observer<in T>) : ObserverWrapper(observer) {
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
            sDefaultScheduler.dispatch {
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
            mOwner.backgroundLifecycle.removeObserver(this)
        }

    }

    private inner class ThreadValueMap {

        private val mValues = WeakHashMap<Thread, ThreadValue?>()

        fun setValueAndGet(value: T) = ThreadValue(Thread.currentThread(), value).apply { mValues[Thread.currentThread()] = this }

        fun get(): ThreadValue? = mValues[Thread.currentThread()]

        inner class ThreadValue(private val thread: Thread, var value: T) {
            fun remove() {
                mValues.remove(thread)
            }
        }
    }
}

/**
 * A event dispatcher interface for [BackgroundLiveEvent].
 */
interface EventDispatcher {
    companion object {
        val BACKGROUND : EventDispatcher = InternalDispatcher("background-event-dispatcher")
        val ASYNC : EventDispatcher = InternalAsyncDispatcher()
        val MAIN : EventDispatcher = InternalMainDispatcher()
    }

    /**
     * Calling when [BackgroundLiveEvent] dispatch event.
     */
    fun dispatch(runnable: Runnable)
}

/**
 * A simple callback that can specified thread to receive event from [BackgroundLiveEvent].
 *
 * @property dispatcher [onChanged] be final called by [EventDispatcher.dispatch].
 * @param T The type of the parameter
 *
 * @see BackgroundLiveEvent BackgroundLiveEvent - for a usage description.
 * @see EventDispatcher     EventDispatcher - has default implementation.
 */
abstract class BackgroundObserver<T> (internal val dispatcher: EventDispatcher) : Observer<T>