package wang.lifecycle.test

import androidx.lifecycle.Observer
import wang.lifecycle.MutableBackgroundLiveEvent
import wang.lifecycle.test.base.BaseTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackgroundLiveEventTest : BaseTest() {

    @Test
    fun MutableBackgroundLiveEvent_observe_testSetValueB2F() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "MutableBackgroundLiveEvent_observe_testSetValueB2F",
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
    fun MutableBackgroundLiveEvent_observeNoSticky_testSetValueB2F() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "MutableBackgroundLiveEvent_observeNoSticky_testSetValueB2F",
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
    fun MutableBackgroundLiveEvent_observeNoLoss_testSetValueB2F() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "MutableBackgroundLiveEvent_observeNoLoss_testSetValueB2F",
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
    fun MutableBackgroundLiveEvent_observeNoStickyNoLoss_testSetValueB2F() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "MutableBackgroundLiveEvent_observeNoStickyNoLoss_testSetValueB2F",
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
    fun MutableBackgroundLiveEvent_observe_testPostValue() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "MutableBackgroundLiveEvent_observe_testPostValue",
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
    fun MutableBackgroundLiveEvent_observeNoSticky_testPostValue() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "MutableBackgroundLiveEvent_observeNoSticky_testPostValue",
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
    fun MutableBackgroundLiveEvent_observeNoLoss_testPostValue() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "MutableBackgroundLiveEvent_observeNoLoss_testPostValue",
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
    fun MutableBackgroundLiveEvent_observeNoStickyNoLoss_testPostValue() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "MutableBackgroundLiveEvent_observeNoStickyNoLoss_testPostValue",
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
    fun MutableBackgroundLiveEvent_observe_testNestedCallSetValue() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "MutableBackgroundLiveEvent_observe_testNestedCallSetValue",
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
    fun MutableBackgroundLiveEvent_observeNoSticky_testNestedCallSetValue() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "MutableBackgroundLiveEvent_observeNoSticky_testNestedCallSetValue",
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
    fun MutableBackgroundLiveEvent_observeNoLoss_testNestedCallSetValue() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "MutableBackgroundLiveEvent_observeNoLoss_testNestedCallSetValue",
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
    fun MutableBackgroundLiveEvent_observeNoStickyNoLoss_testNestedCallSetValue() {
        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        newObserveTestRunner(
            methodName = "MutableBackgroundLiveEvent_observeNoStickyNoLoss_testNestedCallSetValue",
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

}