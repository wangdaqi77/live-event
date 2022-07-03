package wang.lifecycle

import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.arch.core.internal.SafeIterableMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import wang.lifecycle.internal.InternalMainExecutor
import wang.lifecycle.internal.LiveDataSource

/**
 * [LiveEvent] subclass which may observe other `LiveEvent` objects and react on
 * `OnChanged` events from them.
 *
 * This class correctly propagates its active/inactive states down to source [LiveEvent]
 * objects.
 *
 * Consider the following scenario: we have 2 instances of [LiveEvent], let's name them
 * `liveEvent1` and `liveEvent2`, and we want to merge their emissions in one object:
 * `liveEventMerger`. Then, `liveEvent1` and `liveEvent2` will become sources for
 * the `MediatorLiveEvent liveEventMerger` and every time `onChanged` callback
 * is called for either of them, we set a new value in `liveEventMerger`.
 *
 * ```kotlin
 * val liveEvent1: LiveEvent<Int> = ...
 * val liveEvent2: LiveEvent<Int> = ...
 *
 * val liveEventMerger: MediatorLiveEvent<Int> = MediatorLiveEvent()
 * liveEventMerger.addSource(liveEvent1, Observer { value -> liveEventMerger.setValue(value) })
 * liveEventMerger.addSource(liveEvent2, Observer { value -> liveEventMerger.setValue(value) })
 * ```
 *
 * Let's consider that we only want 10 values emitted by `liveEvent1`, to be
 * merged in the `liveEventMerger`. Then, after 10 values, we can stop listening to `liveEvent1`
 * and remove it as a source.
 * ```kotlin
 * liveEventMerger.addSource(liveEvent1, object : Observer<Int> {
 *      private val count = 1
 *
 *      public override fun onChanged(s: Int) {
 *          count++
 *          liveEventMerger.setValue(s)
 *          if (count > 10) {
 *              liveEventMerger.removeSource(liveEvent1)
 *          }
 *      }
 * });
 * ```
 *
 * @param T The type of data hold by this instance
 */
class MediatorLiveEvent<T> : MutableLiveEvent<T>() {
    private val mLiveEventSources by lazy(LazyThreadSafetyMode.NONE) {
        SafeIterableMap<LiveData<*>, Source<*>>()
    }
    private val mBackgroundLiveEventSources by lazy(LazyThreadSafetyMode.NONE) {
        SafeIterableMap<BackgroundLiveEvent<*>, MediatorBackgroundLiveEvent.Source<*>>()
    }
    private val mLiveDataSources by lazy(LazyThreadSafetyMode.NONE) {
        SafeIterableMap<LiveData<*>, LiveDataSource<*>>()
    }

    /**
     * Starts to listen the given `source` LiveEvent, `onChanged` observer will be called
     * when `source` value was changed.
     *
     *
     * `onChanged` callback will be called only when this `MediatorLiveEvent` is active.
     *
     *  If the given LiveEvent is already added as a source but with a different Observer,
     * [IllegalArgumentException] will be thrown.
     *
     * @param source    the `LiveEvent` to listen to
     * @param onChanged The observer that will receive the events
     * @param S         The type of data hold by `source` LiveEvent
     */
    @MainThread
    fun <S> addSource(source: LiveEvent<S>, onChanged: Observer<in S>) {
        val e = Source(source, onChanged)
        val existing = mLiveEventSources.putIfAbsent(source, e)
        require(!(existing != null && existing.mObserver !== onChanged)) { "This source was already added with the different observer" }
        if (existing != null) {
            return
        }
        if (hasActiveObservers()) {
            e.plug()
        }
    }

    /**
     * Stops to listen the given `LiveEvent`.
     *
     * @param toRemote `LiveEvent` to stop to listen
     * @param S        the type of data hold by `source` LiveEvent
     */
    @MainThread
    fun <S> removeSource(toRemote: LiveEvent<S>) {
        val source = mLiveEventSources.remove(toRemote)
        source?.unplug()
    }

    fun <S> addSource(source: BackgroundLiveEvent<S>, onChanged: BackgroundObserver<in S>) {
        val e = MediatorBackgroundLiveEvent.Source(source, onChanged)
        val existing = mBackgroundLiveEventSources.putIfAbsent(source, e)
        require(!(existing != null && existing.mObserver !== onChanged)) { "This source was already added with the different observer" }
        if (existing != null) {
            return
        }
        if (hasActiveObservers()) {
            e.plug()
        }
    }

    fun <S> removeSource(toRemote: BackgroundLiveEvent<S>) {
        val source = mBackgroundLiveEventSources.remove(toRemote)
        source?.unplug()
    }

    @MainThread
    fun <S> addSource(source: LiveData<S>, onChanged: Observer<in S>) {
        val e = LiveDataSource(source, onChanged)
        val existing = mLiveDataSources.putIfAbsent(source, e)
        require(!(existing != null && existing.mObserver !== onChanged)) { "This source was already added with the different observer" }
        if (existing != null) {
            return
        }
        if (hasActiveObservers()) {
            e.plug()
        }
    }

    @MainThread
    fun <S> removeSource(toRemote: LiveData<S>) {
        val source = mLiveDataSources.remove(toRemote)
        source?.unplug()
    }

    @CallSuper
    override fun onActive() {
        for ((_, value) in mLiveEventSources) {
            value.plug()
        }
        for ((_, value) in mBackgroundLiveEventSources) {
            value.plug()
        }
        for ((_, value) in mLiveDataSources) {
            value.plug()
        }
    }

    @CallSuper
    override fun onInactive() {
        for ((_, value) in mLiveEventSources) {
            value.unplug()
        }
        for ((_, value) in mBackgroundLiveEventSources) {
            value.unplug()
        }
        for ((_, value) in mLiveDataSources) {
            value.unplug()
        }
    }

    internal class Source<V> constructor(
        private val mLiveEvent: LiveEvent<V>,
        val mObserver: Observer<in V>
    ) : Observer<V> {
        var mVersion = START_VERSION
        fun plug() {
            if (mVersion != mLiveEvent.version) {
                mLiveEvent.observeForeverNoLoss(this)
            } else {
                mLiveEvent.observeForeverNoStickyNoLoss(this)
            }
        }

        fun unplug() {
            mLiveEvent.removeObserver(this)
        }

        override fun onChanged(v: V) {
            val version = mLiveEvent.version
            if (mVersion != version) {
                mVersion = version
            }
            mObserver.onChanged(v)
        }
    }
}
