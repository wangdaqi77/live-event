package wang.lifecycle.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import wang.lifecycle.MediatorBackgroundLiveEvent
import wang.lifecycle.MediatorLiveEvent
import wang.lifecycle.MutableBackgroundLiveEvent
import wang.lifecycle.MutableLiveEvent
import wang.lifecycle.test.base.BaseTest

@RunWith(AndroidJUnit4::class)
class MediatorTest : BaseTest() {
    @Test
    fun test_MediatorLiveEvent_addSource() {
        val liveEvent1 = MutableLiveEvent<String>()
        val liveEvent2 = MutableLiveEvent<String>()

        val liveEventMerger: MediatorLiveEvent<String> = MediatorLiveEvent()
        newObserveTestRunner(
            methodName = "test_MediatorLiveEvent_addSource",
            desc =
            """
                行为：liveEventMerger合并源
                预期：observe依次观察到的数据(A -> B -> C)
            """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                liveEventMerger.addSource(liveEvent1, observer)
                liveEventMerger.addSource(liveEvent2, observer)
            }
            .resumeActivityThen {
                liveEvent1.setValue("A")
                liveEvent2.setValue("B")
                liveEvent2.setValue("C")
            }
            .destroyActivityThen {
                liveEventMerger.removeSource(liveEvent1)
                liveEventMerger.removeSource(liveEvent2)
            }
            .assertInTurnObserved("A", "B", "C")
    }

    @Test
    fun test_MediatorBackgroundLiveEvent_addSource() {
        val liveEvent1 = MutableBackgroundLiveEvent<String>()
        val liveEvent2 = MutableBackgroundLiveEvent<String>()

        val liveEventMerger: MediatorBackgroundLiveEvent<String> = MediatorBackgroundLiveEvent()
        newObserveTestRunner(
            methodName = "test_MediatorLiveEvent_addSource",
            desc =
            """
                行为：liveEventMerger合并源
                预期：observe依次观察到的数据(A -> B -> C)
            """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                liveEventMerger.addSource(liveEvent1, observer)
                liveEventMerger.addSource(liveEvent2, observer)
            }
            .resumeActivityThen {
                liveEvent1.setValue("A")
                liveEvent2.setValue("B")
                liveEvent2.setValue("C")
            }
            .destroyActivityThen {
                liveEventMerger.removeSource(liveEvent1)
                liveEventMerger.removeSource(liveEvent2)
            }
            .assertInTurnObserved("A", "B", "C")
    }
}