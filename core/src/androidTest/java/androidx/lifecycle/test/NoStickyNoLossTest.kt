package androidx.lifecycle.test

import androidx.lifecycle.NoStickyNoLossBackgroundLiveEvent
import androidx.lifecycle.NoStickyNoLossLiveEvent
import androidx.lifecycle.test.base.BaseTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoStickyNoLossTest: BaseTest() {

    @Test
    fun NoStickyNoLossLiveEvent_testSetValueB2F() {
        val liveEvent = NoStickyNoLossLiveEvent(EVENT_INIT)

        newTestRunner("NoStickyNoLossLiveEvent_testSetValueB2F")
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
            .startAssertChangedOrder( "A", "B", "C")
    }

    @Test
    fun NoStickyNoLossBackgroundLiveEvent_testSetValueB2F() {
        val liveEvent =
            NoStickyNoLossBackgroundLiveEvent(EVENT_INIT)
        newTestRunner("NoStickyNoLossBackgroundLiveEvent_testSetValueB2F")
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
            .startAssertChangedOrder( "A", "B", "C")
    }

    @Test
    fun NoStickyNoLossLiveEvent_testPostValue() {

        val liveEvent = NoStickyNoLossLiveEvent(EVENT_INIT)

        newTestRunner("NoStickyNoLossLiveEvent_testPostValue")
            .runActivity { activity, observer ->
                liveEvent.observe(activity, observer)
                liveEvent.postValue("A")
                liveEvent.postValue("B")
                liveEvent.postValue("C")
            }
            .startAssertChangedOrder("A", "B", "C")
    }

    @Test
    fun NoStickyNoLossBackgroundLiveEvent_testPostValue() {
        val liveEvent =
            NoStickyNoLossBackgroundLiveEvent(EVENT_INIT)

        newTestRunner("NoStickyNoLossBackgroundLiveEvent_testPostValue")
            .runActivity { activity, observer ->
                liveEvent.observe(activity, observer)
                liveEvent.postValue("A")
                liveEvent.postValue("B")
                liveEvent.postValue("C")
            }
            .startAssertChangedOrder("A", "B", "C")
    }

    @Test
    fun NoStickyNoLossLiveEvent_testNestedCallSetValue() {
        val liveEvent = NoStickyNoLossLiveEvent(EVENT_INIT)

        newTestRunner("NoStickyNoLossLiveEvent_testNestedCallSetValue")
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
            .startAssertChangedOrder("A", "Nested", "B", "C")
    }

    @Test
    fun NoStickyNoLossBackgroundLiveEvent_testNestedCallSetValue() {
        val liveEvent =
            NoStickyNoLossBackgroundLiveEvent(EVENT_INIT)

        newTestRunner("NoStickyNoLossBackgroundLiveEvent_testNestedCallSetValue")
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
            .startAssertChangedOrder("A", "Nested", "B", "C")
    }
}