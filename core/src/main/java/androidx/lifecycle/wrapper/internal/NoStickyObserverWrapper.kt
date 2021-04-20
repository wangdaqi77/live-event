package androidx.lifecycle.wrapper.internal

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

internal class AlwaysActiveNoStickyObserverWrapper<T> constructor(
    observer: Observer<in T>,
    override var ignored: Boolean
) : NoStickyObserverWrapper<T>(observer), AlwaysActive


internal class LifecycleBoundNoStickyObserverWrapper<T> internal constructor(private val observers: Observers<T>, private val owner: LifecycleOwner, observer: Observer<in T>, override var ignored: Boolean)
    : NoStickyObserverWrapper<T>(observer) , LifecycleEventObserver {

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            observers.removeObserver(observer)
            return
        }
    }

    override fun isAttachedTo(owner: LifecycleOwner): Boolean  = this.owner === owner

    override fun detachObserver() {
        owner.lifecycle.removeObserver(this)
        super.detachObserver()
    }
}

internal abstract class NoStickyObserverWrapper<T>(override val observer: Observer<in T>) :
    Observer<T>, ObserverBox<T> {
    abstract var ignored: Boolean
    private fun shouldIgnored(): Boolean {
        val ignored = this.ignored
        this.ignored = false
        return ignored
    }

    override fun onChanged(value: T) {
        if (shouldIgnored()) return
        observer.onChanged(value)
    }

    open override fun isAttachedTo(owner: LifecycleOwner): Boolean = false

    open fun detachObserver() {}
}
