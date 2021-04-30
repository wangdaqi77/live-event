package androidx.lifecycle.internal

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

internal interface ObserverBox<T>: Observer<T> {
    val observer: Observer<in T>
    fun isAttachedTo(owner: LifecycleOwner): Boolean
}
