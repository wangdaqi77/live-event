package androidx.lifecycle.internal

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

internal class AlwaysActiveNoStickyObserverBox<T> constructor(
    observer: Observer<in T>,
    override var ignored: Boolean,
    onChanged: (Observer<in T>, T) -> Unit
) : NoStickyObserverBox<T>(observer, onChanged), AlwaysActive


internal class LifecycleBoundNoStickyObserverBox<T> constructor(
    private val mOwner: LifecycleOwner, observer: Observer<in T>, override var ignored: Boolean,
    onChanged: (Observer<in T>, T) -> Unit,
    private val onOwnerDestroy: (Observer<in T>) -> Unit,
    private val isBackground: Boolean = false
) : NoStickyObserverBox<T>(observer, onChanged), LifecycleEventObserver {

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (mOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            onOwnerDestroy(observer)
            return
        }
    }

    override fun isAttachedTo(owner: LifecycleOwner): Boolean  = this.mOwner === owner

    override fun detachObserver() {
        if (isBackground) {
            InternalReflect.removeObserverSafe(mOwner, this)
        }else {
            mOwner.lifecycle.removeObserver(this)
        }
        super.detachObserver()
    }
}

internal abstract class NoStickyObserverBox<T> constructor(
    override val observer: Observer<in T>,
    private val onChanged: (Observer<in T>, T) -> Unit
) :
    Observer<T>, ObserverBox<T> {
    abstract var ignored: Boolean
    private fun shouldIgnored(): Boolean {
        val ignored = this.ignored
        this.ignored = false
        return ignored
    }

    override fun onChanged(value: T) {
        if (shouldIgnored()) return
        onChanged(observer, value)
    }

    open override fun isAttachedTo(owner: LifecycleOwner): Boolean = false

    open fun detachObserver() {}
}
