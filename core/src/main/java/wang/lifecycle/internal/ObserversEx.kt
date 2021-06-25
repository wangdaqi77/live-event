package wang.lifecycle.internal

import androidx.arch.core.internal.SafeIterableMap
import androidx.lifecycle.Observer

internal inline fun<T> SafeIterableMap<Observer<in T>, *>.eachObserver(block:(Observer<in T>) -> Unit){
    @Suppress("INACCESSIBLE_TYPE")
    val iterator = iteratorWithAdditions() as Iterator<Map.Entry<Observer<in T>, *>>
    while (iterator.hasNext()) {
        val observer = iterator.next().key
        block(observer)
    }
}

internal inline fun<T> SafeIterableMap<Observer<in T>, *>.eachObserverBox(block:(ObserverBox<in T>) -> Unit) {
    eachObserver{ value->
        var observer = value
        while (observer is ObserverBox) {
            block(observer)
            observer = observer.observer
        }
    }
}

internal inline fun<T> SafeIterableMap<Observer<in T>, *>.findObserverBox(predicate: (ObserverBox<in T>) -> Boolean): ObserverBox<in T>? {
    eachObserverBox{ observerBox->
        if (predicate(observerBox)) return observerBox
    }
    return null
}

/**
 * @return final observer of observe.
 */
internal fun<T> SafeIterableMap<Observer<in T>, *>.detachObserverBoxWith(observer: Observer<in T>, onDetach:(ObserverBox<in T>)->Unit): Observer<in T> {
    var currentObserver: Observer<in T> = observer
    var currentObserverBox: ObserverBox<in T>?
    do {
        currentObserverBox = findObserverBox { it.observer === currentObserver }
            ?.apply {
                onDetach(this)
                currentObserver = this
            }
    }while (currentObserverBox != null)

    return currentObserver
}