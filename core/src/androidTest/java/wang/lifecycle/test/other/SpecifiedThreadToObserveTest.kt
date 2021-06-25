package wang.lifecycle.test.other

import android.os.Looper
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
            liveEvent.observe(it, object : BackgroundObserver<String>(){
                override fun onChanged(t: String?) {
                    assertEquals("default-dispatcher", Thread.currentThread().name)
                }
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
                    assertEquals("background-event-dispatcher", Thread.currentThread().name)
                }
            })


        }
    }

}