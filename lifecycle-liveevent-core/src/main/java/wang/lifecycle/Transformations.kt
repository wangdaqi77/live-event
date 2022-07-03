@file:JvmName("Transformations")
package wang.lifecycle

import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import wang.lifecycle.*

/**
 * Transformation methods for [LiveEvent].
 *
 * Returns a [LiveEvent] mapped from the input `source` [LiveEvent] by applying
 * [mapFunction] to each value set on `source`.
 *
 * This method is analogous to [io.reactivex.Observable.map].
 *
 * [mapFunction] will be executed on the main thread.
 *
 * Here is an example mapping a simple `User` struct in a [LiveEvent] to a
 * [LiveEvent] containing their full name as a [String].
 *
 * ```kotlin
 * val userLiveEvent: LiveEvent<User> = ...
 * val userFullNameLiveEvent: LiveEvent<String> =
 *     userLiveEvent.map{ user ->
 *         user.firstName + user.lastName
 *     }
 * ```
 *
 * @param mapFunction a function to apply to each value set on `source` in order to set
 * it on the output [LiveEvent]
 * @param X           the generic type parameter of `source`
 * @param Y           the generic type parameter of the returned [LiveEvent]
 * @return a LiveEvent mapped from `source` to type [Y] by applying
 * [mapFunction] to each value set.
 */
@MainThread
fun <X, Y> LiveEvent<X>.map(mapFunction: (X) -> Y): LiveEvent<Y> {
    val result = MediatorLiveEvent<Y>()
    result.addSource(this) { x ->
        result.setValue(mapFunction(x))
    }
    return result
}

/**
 * Transformation methods for [LiveEvent].
 *
 * Returns a [LiveEvent] mapped from the input `source` [LiveEvent] by applying
 * [switchMapFunction] to each value set on `source`.
 *
 * The returned [LiveEvent] delegates to the most recent [LiveEvent] created by
 * calling [switchMapFunction] with the most recent value set to `source`, without
 * changing the reference. In this way, [switchMapFunction] can change the 'backing'
 * [LiveEvent] transparently to any observer registered to the [LiveEvent] returned
 * by `switchMap()`.
 *
 * Note that when the backing [LiveEvent] is switched, no further values from the older
 * [LiveEvent] will be set to the output [LiveEvent]. In this way, the method is
 * analogous to [io.reactivex.Observable.switchMap].
 *
 * [switchMapFunction] will be executed on the main thread.
 *
 * Here is an example class that holds a typed-in name of a user
 * [String] (such as from an `EditText`) in a [MutableLiveEvent] and
 * returns a [LiveEvent] containing a List of `User` objects for users that have
 * that name. It populates that [LiveEvent] by requerying a repository-pattern object
 * each time the typed name changes.
 *
 * This `ViewModel` would permit the observing UI to update "live" as the user ID text
 * changes.
 * ```kotlin
 * class UserViewModel : AndroidViewModel {
 *     val nameQueryLiveEvent: MutableLiveEvent<String> = ...
 *
 *     fun getUsersWithNameLiveEvent() : LiveEvent<List<String>> {
 *         return nameQueryLiveEvent.switchMap { name ->
 *             myDataSource.getUsersWithNameLiveEvent(name)
 *         }
 *     }
 *
 *     fun setNameQuery(String name) {
 *         this.nameQueryLiveE
 *     }
 * }
 * ```
 *
 * @param switchMapFunction a function to apply to each value set on `source` to create a
 *                          new delegate [LiveEvent] for the returned one
 * @param X                 the generic type parameter of `source`
 * @param Y                 the generic type parameter of the returned [LiveEvent]
 *
 * @return a LiveEvent mapped from `source` to type [Y] by delegating to the LiveEvent
 *         returned by applying [switchMapFunction] to each value set
 */
@MainThread
fun <X, Y> LiveEvent<X>.switchMap(switchMapFunction: (X) -> LiveEvent<Y>): LiveEvent<Y> {
    val result = MediatorLiveEvent<Y>()
    result.addSource(this, object : Observer<X> {
        var mSource: LiveEvent<Y>? = null
        override fun onChanged(x: X) {
            val newLiveEvent = switchMapFunction(x)
            if (mSource === newLiveEvent) {
                return
            }
            if (mSource != null) {
                result.removeSource(mSource!!)
            }
            mSource = newLiveEvent
            if (mSource != null) {
                result.addSource(mSource!!) { y ->
                    result.setValue(y)
                }
            }
        }
    })
    return result
}

/**
 * Transformation methods for [BackgroundLiveEvent].
 * @see [map]
 */
fun <X, Y> BackgroundLiveEvent<X>.map(mapFunction: (X) -> Y): BackgroundLiveEvent<Y> {
    val result = MediatorBackgroundLiveEvent<Y>()
    result.addSource(this, BackgroundObserver { x ->
        result.setValue(mapFunction(x))
    })
    return result
}

/**
 * Transformation methods for [BackgroundLiveEvent].
 * @see [switchMap]
 */
fun <X, Y> BackgroundLiveEvent<X>.switchMap(switchMapFunction: (X) -> BackgroundLiveEvent<Y>): BackgroundLiveEvent<Y> {
    val result = MediatorBackgroundLiveEvent<Y>()
    result.addSource(this, BackgroundObserver(EventDispatcher.DEFAULT, object: Observer<X> {
        var mSource: BackgroundLiveEvent<Y>? = null
        override fun onChanged(x: X) {
            val newLiveEvent = switchMapFunction(x)
            if (mSource === newLiveEvent) {
                return
            }
            if (mSource != null) {
                result.removeSource(mSource!!)
            }
            mSource = newLiveEvent
            if (mSource != null) {
                result.addSource(mSource!!, BackgroundObserver { y ->
                    result.setValue(y)
                })
            }
        }
    }))
    return result
}
