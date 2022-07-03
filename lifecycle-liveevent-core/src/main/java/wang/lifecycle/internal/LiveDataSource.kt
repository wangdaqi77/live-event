package wang.lifecycle.internal

import androidx.lifecycle.InternalSupportedLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

internal class LiveDataSource<V> constructor(
    private val mLiveData: LiveData<V>,
    val mObserver: Observer<in V>
) : Observer<V> {
    var mVersion = InternalSupportedLiveData.START_VERSION
    fun plug() {
        mLiveData.observeForever(this)
    }

    fun unplug() {
        mLiveData.removeObserver(this)
    }

    override fun onChanged(v: V) {
        val version = InternalReflect.getVersion(mLiveData)
        if (mVersion != version) {
            mVersion = version
            mObserver.onChanged(v)
        }
    }
}