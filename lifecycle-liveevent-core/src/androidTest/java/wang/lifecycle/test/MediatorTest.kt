package wang.lifecycle.test

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import wang.lifecycle.*
import wang.lifecycle.test.base.BaseTest

@RunWith(AndroidJUnit4::class)
class MediatorTest : BaseTest() {
    @Test
    fun test_MediatorLiveEvent_addSource() {
        val liveEvent1 = MutableLiveEvent<String>()
        val liveData2 = MutableLiveData<String>()
        val liveEvent3 = MutableBackgroundLiveEvent<String>()

        val liveEventMerger: MediatorLiveEvent<String> = MediatorLiveEvent()
        liveEventMerger.addSource(liveEvent1) { liveEventMerger.postValue(it) }
        liveEventMerger.addSource(liveData2) { liveEventMerger.postValue(it) }
        liveEventMerger.addSource(liveEvent3, BackgroundObserver(EventDispatcher.MAIN) {
            liveEventMerger.postValue(it)
        })

        var tempObserver : Observer<String>? = null
        newObserveTestRunner(
            methodName = "test_MediatorLiveEvent_addSource",
            desc =
            """
            行为：liveEventMerger合并源
            预期：observe依次观察到的数据(A -> B -> C)
        """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                tempObserver = observer
                liveEventMerger.observeNoStickyNoLoss(activity, observer)
            }
            .stopActivityThen {  }
            .resumeActivityThen {
                liveEvent1.postValue("A")
                liveData2.postValue("B")
                liveEvent3.postValue("C")
            }
            .stopActivityThen2 { liveEventMerger.removeObservers(it) }
            .resumeActivityThen2 {
                liveEventMerger.observeNoStickyNoLoss(it, tempObserver!!)
            }
            .stopActivityThen3 {

            }
            .resumeActivityThen3 {
                liveEvent1.postValue("A")
                liveData2.postValue("B")
                liveEvent3.postValue("C")
            }
            .assertInTurnObserved("A", "B", "C", "A", "B", "C")
    }

    @Test
    fun test_MediatorBackgroundLiveEvent_addSource() {
        val liveEvent1 = MutableLiveEvent<String>()
        val liveData2 = MutableLiveData<String>()
        val liveEvent3 = MutableBackgroundLiveEvent<String>()

        val liveEventMerger: MediatorBackgroundLiveEvent<String> = MediatorBackgroundLiveEvent()
        liveEventMerger.addSource(liveEvent1) { liveEventMerger.postValue(it) }
        liveEventMerger.addSource(liveData2) { liveEventMerger.postValue(it) }
        liveEventMerger.addSource(liveEvent3, BackgroundObserver(EventDispatcher.MAIN) {
            liveEventMerger.postValue(it)
        })

        var tempObserver : BackgroundObserver<String>? = null
        newObserveTestRunner(
            methodName = "test_MediatorBackgroundLiveEvent_addSource",
            desc =
            """
            行为：liveEventMerger合并源
            预期：observe依次观察到的数据(A -> B -> C)
        """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                tempObserver = observer
                liveEventMerger.observeNoStickyNoLoss(activity, observer)
            }
            .stopActivityThen {  }
            .resumeActivityThen {
                liveEvent1.postValue("A")
                liveData2.postValue("B")
                liveEvent3.postValue("C")
            }
            .stopActivityThen2 { liveEventMerger.removeObservers(it) }
            .resumeActivityThen2 {
                liveEventMerger.observeNoStickyNoLoss(it, tempObserver!!)
            }
            .stopActivityThen3 {

            }
            .resumeActivityThen3 {
                liveEvent1.postValue("A")
                liveData2.postValue("B")
                liveEvent3.postValue("C")
            }
            .assertInTurnObserved("A", "B", "C", "A", "B", "C")
    }
}