package wang.lifecycle.internal

import androidx.arch.core.internal.SafeIterableMap
import androidx.lifecycle.Observer

internal inline fun <T, O : Observer<in T>> SafeIterableMap<O, *>.eachObserver(block:(O) -> Unit) {
    @Suppress("INACCESSIBLE_TYPE")
    val iterator = iteratorWithAdditions() as Iterator<Map.Entry<O, *>>
    while (iterator.hasNext()) {
        val observer = iterator.next().key
        block(observer)
    }
}

internal inline fun <T, O : Observer<in T>> SafeIterableMap<O, *>.eachObserverBox(block:(ObserverBox<in T>) -> Unit) {
    eachObserver{ key : O ->
        var observer = key
        @Suppress("UNCHECKED_CAST")
        while (observer is ObserverBox<*>) {
            block(observer as ObserverBox<in T>)
            observer = observer.observer as O
        }
    }
}

internal inline fun <T, O : Observer<in T>> SafeIterableMap<O, *>.findObserverBox(predicate: (ObserverBox<in T>) -> Boolean): ObserverBox<in T>? {
    eachObserverBox{ observerBox->
        if (predicate(observerBox)) return observerBox
    }
    return null
}

/**
 * @return final observer of observe.
 */
internal fun <T, O : Observer<in T>> SafeIterableMap<O, *>.detachObserverBoxWith(observer: O, onDetach:(ObserverBox<in T>)->Unit): O {
    var currentObserver: Observer<in T> = observer
    var currentObserverBox: ObserverBox<in T>?
    do {
        currentObserverBox = findObserverBox { it.observer === currentObserver }
            ?.apply {
                onDetach(this)
                currentObserver = this
            }
    }while (currentObserverBox != null)

    @Suppress("UNCHECKED_CAST")
    return currentObserver as O
}