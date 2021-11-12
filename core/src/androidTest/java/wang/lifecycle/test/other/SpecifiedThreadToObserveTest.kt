package wang.lifecycle.test.other

import android.os.Looper
import androidx.lifecycle.Observer
import wang.lifecycle.BackgroundObserver
import wang.lifecycle.EventDispatcher
import wang.lifecycle.test.base.BaseTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import wang.lifecycle.MutableBackgroundLiveEvent
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class SpecifiedThreadToObserveTest : BaseTest() {

    @Test
    fun BackgroundLiveEvent_testSpecifiedThreadToObserve() {

        val liveEvent = MutableBackgroundLiveEvent(EVENT_INIT)

        scenario.onActivity {
            liveEvent.observe(it, Observer {
                assertEquals(
                    expected = "default-dispatcher",
                    actual = Thread.currentThread().name,
                    message = "使用Observer类型，应该是统一分发事件的后台线程"
                )
            })

            liveEvent.observe(it, object : BackgroundObserver<String>(
                EventDispatcher.MAIN){
                override fun onChanged(t: String?) {
                    assertEquals(Looper.getMainLooper().thread, Thread.currentThread())
                }
            })

            liveEvent.observe(it, object : BackgroundObserver<String>(
                EventDispatcher.BACKGROUND){
                override fun onChanged(t: String?) {
                    assertEquals(
                        expected = "background-event-dispatcher",
                        actual = Thread.currentThread().name,
                        message = "使用BackgroundObserver类型并且使用EventDispatcher.BACKGROUND，应该是EventDispatcher.BACKGROUND指定的线程"
                    )
                }
            })


        }
    }

}