package wang.lifecycle.test.other

import androidx.lifecycle.Observer
import wang.lifecycle.MutableBackgroundLiveEvent
import wang.lifecycle.MutableLiveEvent
import wang.lifecycle.test.base.BaseTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ActiveTest : BaseTest() {

    @Test
    fun MutableLiveEvent_testActive() {

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
    fun MutableBackgroundLiveEvent_testActive() {

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

            val observer1 = Observer<String> {  }
            val observer2 = Observer<String> {  }

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

            liveEvent.observe(it, Observer {  })
            Thread.sleep(100)
            assertTrue(active)
        }

        scenario.close()
        Thread.sleep(100)
        assertFalse(active)
    }

}