package androidx.lifecycle.test

import androidx.lifecycle.NoStickyBackgroundLiveEvent
import androidx.lifecycle.NoStickyLiveEvent
import androidx.lifecycle.test.base.BaseTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoStickyTest: BaseTest() {

    @Test
    fun NoStickyLiveEvent_testSetValueB2F() {
        val liveEvent = NoStickyLiveEvent(EVENT_INIT)

        newTestRunner("NoStickyLiveEvent_testSetValueB2F")
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
            .startAssertChangedOrder("B", "C")
    }

    @Test
    fun NoStickyBackgroundLiveEvent_testSetValueB2F() {
        val liveEvent =
            NoStickyBackgroundLiveEvent(EVENT_INIT)
        newTestRunner("NoStickyBackgroundLiveEvent_testSetValueB2F")
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
            .startAssertChangedOrder("B", "C")
    }


    @Test
    fun NoStickyLiveEvent_testPostValue() {
        val liveEvent = NoStickyLiveEvent(EVENT_INIT)

        newTestRunner("NoStickyLiveEvent_testPostValue")
            .runActivity { activity, observer ->
                liveEvent.observe(activity, observer)
                liveEvent.postValue("A")
                liveEvent.postValue("B")
                liveEvent.postValue("C")
            }
            .startAssertChangedOrder("C")
    }

    @Test
    fun NoStickyBackgroundLiveEvent_testPostValue() {
        val liveEvent =
            NoStickyBackgroundLiveEvent(EVENT_INIT)

        newTestRunner("NoStickyBackgroundLiveEvent_testPostValue")
            .runActivity { activity, observer ->
                liveEvent.observe(activity, observer)
                liveEvent.postValue("A")
                liveEvent.postValue("B")
                liveEvent.postValue("C")
            }
            .startAssertChangedOrder("C")
    }

    @Test
    fun NoStickyLiveEvent_testNestedCallSetValue() {

        val liveEvent = NoStickyLiveEvent(EVENT_INIT)

        newTestRunner("NoStickyLiveEvent_testNestedCallSetValue")
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
            .startAssertChangedOrder("Nested", "B", "C")
    }

    @Test
    fun NoStickyBackgroundLiveEvent_testNestedCallSetValue() {
        val liveEvent =
            NoStickyBackgroundLiveEvent(EVENT_INIT)

        newTestRunner("NoStickyBackgroundLiveEvent_testNestedCallSetValue")
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
            .startAssertChangedOrder("Nested", "B", "C")
    }
}