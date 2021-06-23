package androidx.lifecycle.test

import androidx.lifecycle.LikeLiveDataBackgroundLiveEvent
import androidx.lifecycle.LikeLiveDataLiveEvent
import androidx.lifecycle.test.base.BaseTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LikeLiveDataTest : BaseTest() {

    @Test
    fun LikeLiveDataLiveEvent_testSetValueB2F() {
        val liveEvent = LikeLiveDataLiveEvent(EVENT_INIT)

        newTestRunner("LikeLiveDataLiveEvent_testSetValueB2F")
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
    fun LikeLiveDataBackgroundLiveEvent_testSetValueB2F() {
        val liveEvent =
            LikeLiveDataBackgroundLiveEvent(EVENT_INIT)
        newTestRunner("LikeLiveDataBackgroundLiveEvent_testSetValueB2F")
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
    fun LikeLiveDataLiveEvent_testPostValue() {
        val liveEvent = LikeLiveDataLiveEvent(EVENT_INIT)

        newTestRunner("LikeLiveDataLiveEvent_testPostValue")
            .runActivity { activity, observer ->
                liveEvent.observe(activity, observer)
                liveEvent.postValue("A")
                liveEvent.postValue("B")
                liveEvent.postValue("C")
            }
            .startAssertChangedOrder(EVENT_INIT, "C")
    }

    @Test
    fun LikeLiveDataBackgroundLiveEvent_testPostValue() {
        val liveEvent =
            LikeLiveDataBackgroundLiveEvent(EVENT_INIT)

        newTestRunner("LikeLiveDataBackgroundLiveEvent_testPostValue")
            .runActivity { activity, observer ->
                liveEvent.observe(activity, observer)
                liveEvent.postValue("A")
                liveEvent.postValue("B")
                liveEvent.postValue("C")
            }
            .startAssertChangedOrder(EVENT_INIT, "C")
    }

    @Test
    fun LikeLiveDataLiveEvent_testNestedCallSetValue() {

        val liveEvent = LikeLiveDataLiveEvent(EVENT_INIT)

        newTestRunner("LikeLiveDataLiveEvent_testNestedCallSetValue")
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
    fun LikeLiveDataBackgroundLiveEvent_testNestedCallSetValue() {
        val liveEvent =
            LikeLiveDataBackgroundLiveEvent(EVENT_INIT)

        newTestRunner("LikeLiveDataBackgroundLiveEvent_testNestedCallSetValue")
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
}