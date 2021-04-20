package androidx.lifecycle.wrapper.internal

import androidx.lifecycle.Observer

internal interface Observers<T> {
    fun removeObserver(observer: Observer<in T>)
}