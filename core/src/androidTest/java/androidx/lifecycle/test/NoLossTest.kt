package androidx.lifecycle.test

import androidx.lifecycle.NoLossBackgroundLiveEvent
import androidx.lifecycle.NoLossLiveEvent
import androidx.lifecycle.test.base.BaseTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoLossTest: BaseTest() {

    @Test
    fun NoLossLiveEvent_testSetValueB2F() {
        val liveEvent = NoLossLiveEvent(EVENT_INIT)

        newTestRunner("NoLossLiveEvent_testSetValueB2F")
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
            .startAssertChangedOrder(EVENT_INIT, "A", "B", "C")
    }

    @Test
    fun NoLossBackgroundLiveEvent_testSetValueB2F() {
        val liveEvent = NoLossBackgroundLiveEvent(EVENT_INIT)
        newTestRunner("NoLossBackgroundLiveEvent_testSetValueB2F")
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
            .startAssertChangedOrder(EVENT_INIT, "A", "B", "C")
    }

    @Test
    fun NoLossLiveEvent_testPostValue() {

        val liveEvent = NoLossLiveEvent(EVENT_INIT)

        newTestRunner("NoLossLiveEvent_testPostValue")
            .runActivity { activity, observer ->
                liveEvent.observe(activity, observer)
                liveEvent.postValue("A")
                liveEvent.postValue("B")
                liveEvent.postValue("C")
            }
            .startAssertChangedOrder(EVENT_INIT, "A", "B", "C")
    }

    @Test
    fun NoLossBackgroundLiveEvent_testPostValue() {
        val liveEvent = NoLossBackgroundLiveEvent(EVENT_INIT)

        newTestRunner("NoLossBackgroundLiveEvent_testPostValue")
            .runActivity { activity, observer ->
                liveEvent.observe(activity, observer)
                liveEvent.postValue("A")
                liveEvent.postValue("B")
                liveEvent.postValue("C")
            }
            .startAssertChangedOrder(EVENT_INIT, "A", "B", "C")
    }

    @Test
    fun NoLossLiveEvent_testNestedCallSetValue() {
        val liveEvent = NoLossLiveEvent(EVENT_INIT)

        newTestRunner("NoLossLiveEvent_testNestedCallSetValue")
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
            .startAssertChangedOrder(EVENT_INIT, "A", "Nested", "B", "C")
    }

    @Test
    fun NoLossBackgroundLiveEvent_testNestedCallSetValue() {
        val liveEvent = NoLossBackgroundLiveEvent(EVENT_INIT)

        newTestRunner("NoLossBackgroundLiveEvent_testNestedCallSetValue")
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
            .startAssertChangedOrder(EVENT_INIT, "A", "Nested", "B", "C")
    }
}