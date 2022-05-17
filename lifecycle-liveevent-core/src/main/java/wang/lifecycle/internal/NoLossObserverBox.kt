package wang.lifecycle.internal

import androidx.lifecycle.*
import wang.lifecycle.BackgroundLiveEvent
import java.util.concurrent.atomic.AtomicBoolean

internal class AlwaysActiveNoLossObserverBox<T> constructor(
    observer: Observer<in T>,
    onChanged: (Observer<in T>, T) -> Unit
) : NoLossObserverBox<T>(observer, onChanged), AlwaysActive {
    override fun shouldBeActive(): Boolean {
        return true
    }
}

internal class AlwaysActiveToLifecycleBoundNoLossObserverBox<T> constructor(
    private val mOwner: LifecycleOwner,
    observer: Observer<in T>,
    onChanged: (Observer<in T>, T) -> Unit,
    private val onOwnerDestroy: (Observer<in T>) -> Unit,
    private val isBackground: Boolean = false
) : NoLossObserverBox<T>(observer, onChanged), LifecycleEventObserver {

    override fun shouldBeActive(): Boolean = mOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        var currentState = mOwner.lifecycle.currentState
        if (currentState == Lifecycle.State.DESTROYED) {
            onOwnerDestroy(observer)
            return
        }

        var prevState: Lifecycle.State? = null
        while (prevState != currentState) {
            prevState = currentState
            activeStateChanged(shouldBeActive())
            currentState = mOwner.lifecycle.currentState
        }
    }

    override fun isAttachedTo(owner: LifecycleOwner): Boolean = this.mOwner === owner

    override fun detachObserver() {
        if (isBackground) {
            BackgroundLiveEvent.run {
                mOwner.backgroundLifecycle.removeObserver(this@AlwaysActiveToLifecycleBoundNoLossObserverBox)
            }
        }else {
            mOwner.lifecycle.removeObserver(this)
        }
        super.detachObserver()
    }

}

internal abstract class NoLossObserverBox<T> constructor(
    override val observer: Observer<in T>,
    private val onChanged: (Observer<in T>, T) -> Unit
) : Observer<T>, ObserverBox<T> {

    private var mLostLock = Any()
    private var mActive = false
    private var mLostHead: LostValue.Head<T>? = null

    abstract fun shouldBeActive(): Boolean

    override fun isAttachedTo(owner: LifecycleOwner): Boolean = false

    open fun detachObserver() {
        synchronized(mLostLock) {
            mLostHead = null
        }
    }

    override fun onChanged(value: T) {
        dispatchingValue(value)
    }

    fun activeStateChanged(newActive: Boolean) {
        if (mActive == newActive) return

        mActive = newActive
        if (newActive) {
            dispatchingLostValue()
        }
    }

    private fun dispatchingLostValue() {
        val lostHead:LostValue.Head<T>
        synchronized(mLostLock) {
            lostHead = mLostHead ?: return
            mLostHead = null
        }
        lostHead.eachToTail{ onChanged(observer, it)}
    }

    private fun dispatchingValue(value: T) {
        if (shouldBeActive()) {
            dispatchingLostValue()
            onChanged(observer, value)
        }else {
            // into linked.
            recordLostValue(value)
        }
    }

    private fun recordLostValue(value:T) {
        synchronized(mLostLock) {
            val head = mLostHead
            if (head == null) {
                mLostHead = LostValue.Head(value)
            }else{
                head.appendTail(value)
            }
        }
    }
}
