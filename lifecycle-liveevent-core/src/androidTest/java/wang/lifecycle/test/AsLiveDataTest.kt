package wang.lifecycle.test

import wang.lifecycle.MutableLiveEvent
import wang.lifecycle.test.base.BaseTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import wang.lifecycle.MutableBackgroundLiveEvent

@RunWith(AndroidJUnit4::class)
class AsLiveDataTest : BaseTest() {

    @Test
    fun test_LiveEvent_asLiveData_observe_byCallSetValueB2F() {
        val liveEvent = MutableLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "test_LiveEvent_asLiveData_observe_byCallSetValueB2F",
            desc =
            """
                行为：liveEvent包含初始事件${EVENT_INIT}，activity启动后调用observe()，然后activity切到后台依次调用setValue(A)、setValue(B)，切回到前台后调用setValue(C)
                预期：observe依次观察到的数据(${EVENT_INIT} -> B -> C)
            """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                liveEvent.asLiveData().observe(activity, observer)
            }
            .stopActivityThen {
                liveEvent.setValue("A")
                liveEvent.setValue("B")
            }
            .resumeActivityThen {
                liveEvent.setValue("C")
            }
            .assertInTurnObserved(EVENT_INIT, "B", "C")

    }

    @Test
    fun test_BackgroundLiveEvent_asLiveData_observe_byCallSetValueB2F() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "test_BackgroundLiveEvent_asLiveData_observe_byCallSetValueB2F",
            desc =
            """
                行为：liveEvent包含初始事件${EVENT_INIT}，activity启动后调用observe()，然后activity切到后台依次调用setValue(A)、setValue(B)，切回到前台后调用setValue(C)
                预期：observe依次观察到的数据(${EVENT_INIT} -> B -> C)
            """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                liveEvent.asLiveData().observe(activity, observer)
            }
            .stopActivityThen {
                liveEvent.setValue("A")
                liveEvent.setValue("B")
            }
            .resumeActivityThen {
                liveEvent.setValue("C")
            }
            .assertInTurnObserved(EVENT_INIT, "B", "C")
    }
}