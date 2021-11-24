package wang.lifecycle.test

import android.os.Looper
import androidx.lifecycle.Observer
import wang.lifecycle.MutableBackgroundLiveEvent
import wang.lifecycle.test.base.BaseTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import wang.lifecycle.BackgroundLiveEvent
import wang.lifecycle.BackgroundObserver
import wang.lifecycle.EventDispatcher
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class BackgroundLiveEventTest : BaseTest() {

    @Test
    fun test_BackgroundLiveEvent_observe_byCallSetValueB2F() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "test_BackgroundLiveEvent_observe_byCallSetValueB2F",
            desc =
            """
                行为：liveEvent包含初始事件${EVENT_INIT}，activity启动后调用observe()，然后activity切到后台依次调用setValue(A)、setValue(B)，切回到前台后调用setValue(C)
                预期：observe依次观察到的数据(${EVENT_INIT} -> B -> C)
            """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                liveEvent.observe(activity, observer)
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
    fun test_BackgroundLiveEvent_observeNoSticky_byCallSetValueB2F() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "test_BackgroundLiveEvent_observeNoSticky_byCallSetValueB2F",
            desc =
            """
                行为：liveEvent包含初始事件${EVENT_INIT}，activity启动后调用observe()，然后activity切到后台依次调用setValue(A)、setValue(B)，切回到前台后调用setValue(C)
                预期：observeNoSticky依次观察到的数据(B -> C)
            """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                liveEvent.observeNoSticky(activity, observer)
            }
            .stopActivityThen {
                liveEvent.setValue("A")
                liveEvent.setValue("B")
            }
            .resumeActivityThen {
                liveEvent.setValue("C")
            }
            .assertInTurnObserved("B", "C")

    }

    @Test
    fun test_BackgroundLiveEvent_observeNoLoss_byCallSetValueB2F() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "test_BackgroundLiveEvent_observeNoLoss_byCallSetValueB2F",
            desc =
            """
                行为：liveEvent包含初始事件${EVENT_INIT}，activity启动后调用observe()，然后activity切到后台依次调用setValue(A)、setValue(B)，切回到前台后调用setValue(C)
                预期：observeNoLoss依次观察到的数据(${EVENT_INIT} -> A -> B -> C)
            """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                liveEvent.observeNoLoss(activity, observer)
            }
            .stopActivityThen {
                liveEvent.setValue("A")
                liveEvent.setValue("B")
            }
            .resumeActivityThen {
                liveEvent.setValue("C")
            }
            .assertInTurnObserved(EVENT_INIT, "A", "B", "C")
    }

    @Test
    fun test_BackgroundLiveEvent_observeNoStickyNoLoss_byCallSetValueB2F() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "test_BackgroundLiveEvent_observeNoStickyNoLoss_byCallSetValueB2F",
            desc =
            """
                行为：liveEvent包含初始事件${EVENT_INIT}，activity启动后调用observe()，然后activity切到后台依次调用setValue(A)、setValue(B)，切回到前台后调用setValue(C)
                预期：observeNoStickyNoLoss依次观察到的数据(A -> B -> C)
            """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                liveEvent.observeNoStickyNoLoss(activity, observer)
            }
            .stopActivityThen {
                liveEvent.setValue("A")
                liveEvent.setValue("B")
            }
            .resumeActivityThen {
                liveEvent.setValue("C")
            }
            .assertInTurnObserved("A", "B", "C")
    }

    @Test
    fun test_BackgroundLiveEvent_observe_byCallPostValue() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "test_BackgroundLiveEvent_observe_byCallPostValue",
            desc =
            """
                行为：liveEvent包含初始事件${EVENT_INIT}，activity启动后调用observe()，然后依次调用postValue(A)、postValue(B)，postValue(C)
                预期：observe依次观察到的数据(${EVENT_INIT} -> C)
            """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                liveEvent.observe(activity, observer)
                liveEvent.postValue("A")
                liveEvent.postValue("B")
                liveEvent.postValue("C")
            }
            .assertInTurnObserved(EVENT_INIT, "C")

    }

    @Test
    fun test_BackgroundLiveEvent_observeNoSticky_byCallPostValue() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "test_BackgroundLiveEvent_observeNoSticky_byCallPostValue",
            desc =
            """
                行为：liveEvent包含初始事件${EVENT_INIT}，activity启动后调用observe()，然后依次调用postValue(A)、postValue(B)，postValue(C)
                预期：observeNoSticky依次观察到的数据(C)
            """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                liveEvent.observeNoSticky(activity, observer)
                liveEvent.postValue("A")
                liveEvent.postValue("B")
                liveEvent.postValue("C")
            }
            .assertInTurnObserved("C")

    }

    @Test
    fun test_BackgroundLiveEvent_observeNoLoss_byCallPostValue() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "test_BackgroundLiveEvent_observeNoLoss_byCallPostValue",
            desc =
            """
                行为：liveEvent包含初始事件${EVENT_INIT}，activity启动后调用observe()，然后依次调用postValue(A)、postValue(B)，postValue(C)
                预期：observeNoLoss依次观察到的数据(${EVENT_INIT} -> A -> B -> C)
            """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                liveEvent.observeNoLoss(activity, observer)
                liveEvent.postValue("A")
                liveEvent.postValue("B")
                liveEvent.postValue("C")
            }
            .assertInTurnObserved(EVENT_INIT, "A", "B", "C")
    }

    @Test
    fun test_BackgroundLiveEvent_observeNoStickyNoLoss_byCallPostValue() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "test_BackgroundLiveEvent_observeNoStickyNoLoss_byCallPostValue",
            desc =
            """
                行为：liveEvent包含初始事件${EVENT_INIT}，activity启动后调用observe()，然后依次调用postValue(A)、postValue(B)，postValue(C)
                预期：observeNoStickyNoLoss依次观察到的数据(A -> B -> C)
            """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                liveEvent.observeNoStickyNoLoss(activity, observer)
                liveEvent.postValue("A")
                liveEvent.postValue("B")
                liveEvent.postValue("C")
            }
            .assertInTurnObserved("A", "B", "C")
    }

    @Test
    fun test_BackgroundLiveEvent_observe_byNestedCallSetValue() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "test_BackgroundLiveEvent_observe_byNestedCallSetValue",
            desc =
            """
                行为：liveEvent包含初始事件${EVENT_INIT}1，activity启动后依次调用observe(Observer1)，observe(Observer2)，然后依次setValue(A)、setValue(B)，setValue(C)，其中在Observer1的onChanged接收到A事件后调用setValue(Nested)
                预期：observe(Observer2)依次观察到的数据(${EVENT_INIT} -> Nested -> B -> C)
            """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                liveEvent.observe(activity, Observer {
                    if (it == "A") {
                        liveEvent.setValue("Nested")
                    }
                })
                liveEvent.observe(activity, observer)
                liveEvent.setValue("A")
                liveEvent.setValue("B")
                liveEvent.setValue("C")
            }
            .assertInTurnObserved(EVENT_INIT, "Nested", "B", "C")
    }

    @Test
    fun test_BackgroundLiveEvent_observeNoSticky_byNestedCallSetValue() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "test_BackgroundLiveEvent_observeNoSticky_byNestedCallSetValue",
            desc =
            """
                行为：liveEvent包含初始事件${EVENT_INIT}1，activity启动后依次调用observe(Observer1)，observe(Observer2)，然后依次setValue(A)、setValue(B)，setValue(C)，其中在Observer1的onChanged接收到A事件后调用setValue(Nested)
                预期：observeNoSticky(Observer2)依次观察到的数据(Nested -> B -> C)
            """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                liveEvent.observeNoSticky(activity, Observer {
                    if (it == "A") {
                        liveEvent.setValue("Nested")
                    }
                })
                liveEvent.observeNoSticky(activity, observer)
                liveEvent.setValue("A")
                liveEvent.setValue("B")
                liveEvent.setValue("C")
            }
            .assertInTurnObserved("Nested", "B", "C")

    }

    @Test
    fun test_BackgroundLiveEvent_observeNoLoss_byNestedCallSetValue() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "test_BackgroundLiveEvent_observeNoLoss_byNestedCallSetValue",
            desc =
            """
                行为：liveEvent包含初始事件${EVENT_INIT}1，activity启动后依次调用observe(Observer1)，observe(Observer2)，然后依次setValue(A)、setValue(B)，setValue(C)，其中在Observer1的onChanged接收到A事件后调用setValue(Nested)
                预期：observeNoLoss(Observer2)依次观察到的数据(${EVENT_INIT} -> A -> Nested -> B -> C)
            """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                liveEvent.observeNoLoss(activity, Observer {
                    if (it == "A") {
                        liveEvent.setValue("Nested")
                    }
                })
                liveEvent.observeNoLoss(activity, observer)
                liveEvent.setValue("A")
                liveEvent.setValue("B")
                liveEvent.setValue("C")
            }
            .assertInTurnObserved(EVENT_INIT, "A", "Nested", "B", "C")
    }

    @Test
    fun test_BackgroundLiveEvent_observeNoStickyNoLoss_byNestedCallSetValue() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "test_BackgroundLiveEvent_observeNoStickyNoLoss_byNestedCallSetValue",
            desc =
            """
                行为：liveEvent包含初始事件${EVENT_INIT}1，activity启动后依次调用observe(Observer1)，observe(Observer2)，然后依次setValue(A)、setValue(B)，setValue(C)，其中在Observer1的onChanged接收到A事件后调用setValue(Nested)
                预期：observeNoStickyNoLoss(Observer2)依次观察到的数据(A -> Nested -> B -> C)
            """.trimIndent()
        )
            .launchActivityThen { activity, observer ->
                liveEvent.observeNoStickyNoLoss(activity, Observer {
                    if (it == "A") {
                        liveEvent.setValue("Nested")
                    }
                })
                liveEvent.observeNoStickyNoLoss(activity, observer)
                liveEvent.setValue("A")
                liveEvent.setValue("B")
                liveEvent.setValue("C")
            }
            .assertInTurnObserved("A", "Nested", "B", "C")
    }


    @Test
    fun test_BackgroundLiveEvent_observeSpecifiedThread_byUseBackgroundObserver() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        scenario.onActivity {
            liveEvent.observe(it, Observer {
                assertEquals(
                    expected = BackgroundLiveEvent.THREAD_NAME_DEFAULT_SCHEDULER,
                    actual = Thread.currentThread().name,
                    message = "使用Observer类型时线程就是BackgroundLiveEvent内置负责调度事件的唯一的子线程。"
                )
            })

            liveEvent.observe(it, object : BackgroundObserver<String>(
                EventDispatcher.MAIN){
                override fun onChanged(t: String?) {
                    assertEquals(
                        Looper.getMainLooper().thread,
                        Thread.currentThread(),
                        "应当是主线程"
                    )
                }
            })

            liveEvent.observe(it, object : BackgroundObserver<String>(
                EventDispatcher.BACKGROUND){
                override fun onChanged(t: String?) {
                    assertEquals(
                        expected = EventDispatcher.THREAD_NAME_BACKGROUND,
                        actual = Thread.currentThread().name,
                        message = "应当是EventDispatcher.BACKGROUND指定的线程"
                    )
                }
            })
        }
    }

}