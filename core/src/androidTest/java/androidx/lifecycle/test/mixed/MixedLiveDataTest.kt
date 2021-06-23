package androidx.lifecycle.test.mixed

import androidx.lifecycle.mixed.MixedLiveEvent
import androidx.lifecycle.test.base.BaseTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MixedLiveDataTest : BaseTest() {

    @Test
    fun MixedLiveEvent_observe_testSetValueB2F() {
        val liveEvent = MixedLiveEvent(EVENT_INIT)

        newTestRunner("MixedLiveEvent_observe_testSetValueB2F")
            .runActivity { activity, observer ->
                liveEvent.observe(activity, observer)
            }
            .stoppedActivity {
                liveEvent.setValue("A")
                liveEvent.setValue("B")
            }
            .startedActivity {
                liveEvent.setValue("C")
            }
            .startAssertChangedOrder(EVENT_INIT, "B", "C")

    }

    @Test
    fun MixedLiveEvent_observeNoSticky_testSetValueB2F() {
        val liveEvent = MixedLiveEvent(EVENT_INIT)

        newTestRunner("MixedLiveEvent_observeNoSticky_testSetValueB2F")
            .runActivity { activity, observer ->
                liveEvent.observeNoSticky(activity, observer)
            }
            .stoppedActivity {
                liveEvent.setValue("A")
                liveEvent.setValue("B")
            }
            .startedActivity {
                liveEvent.setValue("C")
            }
            .startAssertChangedOrder("B", "C")

    }

    @Test
    fun MixedLiveEvent_observeNoLoss_testSetValueB2F() {
        val liveEvent = MixedLiveEvent(EVENT_INIT)

        newTestRunner("MixedLiveEvent_observeNoLoss_testSetValueB2F")
            .runActivity { activity, observer ->
                liveEvent.observeNoLoss(activity, observer)
            }
            .stoppedActivity {
                liveEvent.setValue("A")
                liveEvent.setValue("B")
            }
            .startedActivity {
                liveEvent.setValue("C")
            }
            .startAssertChangedOrder(EVENT_INIT, "A", "B", "C")
    }

    @Test
    fun MixedLiveEvent_observeNoStickyNoLoss_testSetValueB2F() {
        val liveEvent = MixedLiveEvent(EVENT_INIT)

        newTestRunner("MixedLiveEvent_observeNoStickyNoLoss_testSetValueB2F")
            .runActivity { activity, observer ->
                liveEvent.observeNoStickyNoLoss(activity, observer)
            }
            .stoppedActivity {
                liveEvent.setValue("A")
                liveEvent.setValue("B")
            }
            .startedActivity {
                liveEvent.setValue("C")
            }
            .startAssertChangedOrder("A", "B", "C")
    }

    @Test
    fun MixedLiveEvent_observe_testPostValue() {
        val liveEvent = MixedLiveEvent(EVENT_INIT)

        newTestRunner("MixedLiveEvent_observe_testPostValue")
            .runActivity { activity, observer ->
                liveEvent.observe(activity, observer)
                liveEvent.postValue("A")
                liveEvent.postValue("B")
                liveEvent.postValue("C")
            }
            .startAssertChangedOrder(EVENT_INIT, "C")

    }

    @Test
    fun MixedLiveEvent_observeNoSticky_testPostValue() {
        val liveEvent = MixedLiveEvent(EVENT_INIT)

        newTestRunner("MixedLiveEvent_observeNoSticky_testPostValue")
            .runActivity { activity, observer ->
                liveEvent.observeNoSticky(activity, observer)
                liveEvent.postValue("A")
                liveEvent.postValue("B")
                liveEvent.postValue("C")
            }
            .startAssertChangedOrder("C")

    }

    @Test
    fun MixedLiveEvent_observeNoLoss_testPostValue() {
        val liveEvent = MixedLiveEvent(EVENT_INIT)

        newTestRunner("MixedLiveEvent_observeNoLoss_testPostValue")
            .runActivity { activity, observer ->
                liveEvent.observeNoLoss(activity, observer)
                liveEvent.postValue("A")
                liveEvent.postValue("B")
                liveEvent.postValue("C")
            }
            .startAssertChangedOrder(EVENT_INIT, "A", "B", "C")
    }

    @Test
    fun MixedLiveEvent_observeNoStickyNoLoss_testPostValue() {
        val liveEvent = MixedLiveEvent(EVENT_INIT)

        newTestRunner("MixedLiveEvent_observeNoStickyNoLoss_testPostValue")
            .runActivity { activity, observer ->
                liveEvent.observeNoStickyNoLoss(activity, observer)
                liveEvent.postValue("A")
                liveEvent.postValue("B")
                liveEvent.postValue("C")
            }
            .startAssertChangedOrder("A", "B", "C")
    }

    @Test
    fun MixedLiveEvent_observe_testNestedCallSetValue() {
        val liveEvent = MixedLiveEvent(EVENT_INIT)

        newTestRunner("MixedLiveEvent_observe_testNestedCallSetValue")
            .onChangedOfObserver1 {
                if (it == "A") {
                    liveEvent.setValue("Nested")
                }
            }
            .runActivityForNestedCallSetValue { activity, observer1, observer2 ->
                liveEvent.observe(activity, observer1)
                liveEvent.observe(activity, observer2)
                liveEvent.setValue("A")
                liveEvent.setValue("B")
                liveEvent.setValue("C")
            }
            .startAssertChangedOrder(EVENT_INIT, "Nested", "B", "C")

    }

    @Test
    fun MixedLiveEvent_observeNoSticky_testNestedCallSetValue() {
        val liveEvent = MixedLiveEvent(EVENT_INIT)

        newTestRunner("MixedLiveEvent_observeNoSticky_testNestedCallSetValue")
            .onChangedOfObserver1 {
                if (it == "A") {
                    liveEvent.setValue("Nested")
                }
            }
            .runActivityForNestedCallSetValue { activity, observer1, observer2 ->
                liveEvent.observeNoSticky(activity, observer1)
                liveEvent.observeNoSticky(activity, observer2)
                liveEvent.setValue("A")
                liveEvent.setValue("B")
                liveEvent.setValue("C")
            }
            .startAssertChangedOrder("Nested", "B", "C")

    }

    @Test
    fun MixedLiveEvent_observeNoLoss_testNestedCallSetValue() {
        val liveEvent = MixedLiveEvent(EVENT_INIT)

        newTestRunner("MixedLiveEvent_observeNoLoss_testNestedCallSetValue")
            .onChangedOfObserver1 {
                if (it == "A") {
                    liveEvent.setValue("Nested")
                }
            }
            .runActivityForNestedCallSetValue { activity, observer1, observer2 ->
                liveEvent.observeNoLoss(activity, observer1)
                liveEvent.observeNoLoss(activity, observer2)
                liveEvent.setValue("A")
                liveEvent.setValue("B")
                liveEvent.setValue("C")
            }
            .startAssertChangedOrder(EVENT_INIT, "A", "Nested", "B", "C")
    }

    @Test
    fun MixedLiveEvent_observeNoStickyNoLoss_testNestedCallSetValue() {
        val liveEvent = MixedLiveEvent(EVENT_INIT)

        newTestRunner("MixedLiveEvent_observeNoStickyNoLoss_testNestedCallSetValue")
            .onChangedOfObserver1 {
                if (it == "A") {
                    liveEvent.setValue("Nested")
                }
            }
            .runActivityForNestedCallSetValue { activity, observer1, observer2 ->
                liveEvent.observeNoStickyNoLoss(activity, observer1)
                liveEvent.observeNoStickyNoLoss(activity, observer2)
                liveEvent.setValue("A")
                liveEvent.setValue("B")
                liveEvent.setValue("C")
            }
            .startAssertChangedOrder("A", "Nested", "B", "C")
    }

}