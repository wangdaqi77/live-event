package androidx.lifecycle.wrapper.internal

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

internal class AlwaysActiveNoLossObserver<T> constructor(observer: Observer<in T>) :
    NoLossObserverWrapper<T>(observer), AlwaysActive {
    override fun shouldBeActive(): Boolean {
        return true
    }
}


internal class AlwaysActiveToLifecycleBoundNoLossObserver<T> constructor(private val observers: Observers<T>, private val mOwner: LifecycleOwner, observer: Observer<in T>) :
    NoLossObserverWrapper<T>(observer), LifecycleEventObserver {

    override fun shouldBeActive(): Boolean = mOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        var currentState = mOwner.lifecycle.currentState
        if (currentState == Lifecycle.State.DESTROYED) {
            observers.removeObserver(observer)
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
        mOwner.lifecycle.removeObserver(this)
        super.detachObserver()
    }

}

internal abstract class NoLossObserverWrapper<T> internal constructor(override val observer: Observer<in T>) : Observer<T>,
    ObserverBox<T> {

    private var mActive = false
    private var mLostHead : LostValue.Head<T>? = null

    abstract fun shouldBeActive(): Boolean

    override fun isAttachedTo(owner: LifecycleOwner): Boolean = false

    open fun detachObserver() {
        mLostHead = null
    }

    override fun onChanged(value: T) {
        dispatchingValue(value)
    }

    fun activeStateChanged(newActive: Boolean) {
        if (newActive == mActive) {
            return
        }
        mActive = newActive

        if (mActive) { // record loss from active to inActive
            val lostHead = mLostHead
            mLostHead = null
            lostHead?.eachLeft(observer::onChanged)
        }
    }

    private fun dispatchingValue(value: T) {
        if (shouldBeActive()) {
            val lostHead = mLostHead
            mLostHead = null
            lostHead?.eachLeft(observer::onChanged)
            observer.onChanged(value)
        }else {
            // into linked.
            recordLostValue(value)
        }
    }


    fun considerNotifyPossibleLostPendingValue(possibleLostHead: LostValue.Head<T>) {
        possibleLostHead.eachLeft(::onChanged)
    }

    private fun recordLostValue(value:T) {
        var head = this.mLostHead
        if (head == null) {
            head = LostValue.Head(value)
            mLostHead = head
        }else {
            head.appendLatest(value)
        }
    }

}
