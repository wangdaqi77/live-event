package wang.lifecycle.test

import androidx.lifecycle.Observer
import wang.lifecycle.MutableBackgroundLiveEvent
import wang.lifecycle.MutableLiveEvent
import wang.lifecycle.test.base.BaseTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import wang.lifecycle.BackgroundObserver
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ActiveTest : BaseTest() {

    @Test
    fun test_LiveEvent_testActive() {
        // 不要在锁屏状态下测试

        var active = false
        val liveEvent = object : MutableLiveEvent<String>(EVENT_INIT) {
            override fun onActive() {
                active = true
                super.onActive()
            }

            override fun onInactive() {
                active = false
                super.onInactive()
            }
        }


        scenario.onActivity {

            val observer1 = Observer<String> {  }
            val observer2 = Observer<String> {  }

            assertFalse(active)
            liveEvent.observeForever(observer1)
            assertTrue(active)
            liveEvent.observeForever(observer2)
            assertTrue(active)
            liveEvent.removeObserver(observer1)
            assertTrue(active)
            liveEvent.removeObserver(observer2)
            assertFalse(active)

            liveEvent.observe(it, Observer {  })
            assertTrue(active)
        }

        scenario.close()
        assertFalse(active)
    }


    @Test
    fun test_BackgroundLiveEvent_testActive() {
        // 不要在锁屏状态下测试

        var active = false
        val liveEvent = object : MutableBackgroundLiveEvent<String>(EVENT_INIT) {
            override fun onActive() {
                active = true
                super.onActive()
            }

            override fun onInactive() {
                active = false
                super.onInactive()
            }
        }


        scenario.onActivity {

            val observer1 = BackgroundObserver<String> {
            }
            val observer2 = BackgroundObserver<String> {
            }

            assertFalse(active)
            liveEvent.observeForever(observer1)
            Thread.sleep(100)
            assertTrue(active)
            liveEvent.observeForever(observer2)
            Thread.sleep(100)
            assertTrue(active)
            liveEvent.removeObserver(observer1)
            Thread.sleep(100)
            assertTrue(active)
            liveEvent.removeObserver(observer2)
            Thread.sleep(100)
            assertFalse(active)

            liveEvent.observe(it, BackgroundObserver { })
            Thread.sleep(100)
            assertTrue(active)
        }

        scenario.close()
        Thread.sleep(100)
        assertFalse(active)
    }

}