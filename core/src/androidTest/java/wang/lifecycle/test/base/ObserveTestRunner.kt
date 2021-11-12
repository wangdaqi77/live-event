package wang.lifecycle.test.base

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.test.core.app.ActivityScenario
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.assertEquals

class ObserveTestRunner<A : Activity, T>(
    private val scenario: ActivityScenario<A>,
    private val testClassName: String,
    private val testMethodName: String,
    private val desc: String?
) {
    companion object {
        private const val TAG = "TestRunner"

        fun <A : Activity, T> newRunner(
            scenario: ActivityScenario<A>,
            testClassName: String,
            testMethodName: String,
            desc: String?
        ) = ObserveTestRunner<A, T>(
            scenario,
            testClassName,
            testMethodName,
            desc
        )
    }

    private lateinit var _onLaunchActivity: (activity: A, Observer<T>) -> Unit
    private lateinit var onActivityStopped: (activity: A) -> Unit
    private lateinit var onActivityResumed: (activity: A) -> Unit
    private lateinit var onActivityDestroy: (activity: A) -> Unit
    private var _onBefore: (activity: A) -> Unit = {}

    fun launchActivityThen(block: (activity: A, Observer<T>) -> Unit): ObserveTestRunner<A, T> {
        this._onLaunchActivity = block
        return this
    }

    fun stopActivityThen(block: (activity: A) -> Unit): ObserveTestRunner<A, T> {
        this.onActivityStopped = block
        return this
    }

    fun resumeActivityThen(block: (activity: A) -> Unit): ObserveTestRunner<A, T> {
        this.onActivityResumed = block
        return this
    }

    fun destroyActivityThen(block: (activity: A) -> Unit): ObserveTestRunner<A, T> {
        this.onActivityDestroy = block
        return this
    }

    fun assertInTurnObserved(vararg value: T, onFailedMessage: ((seq: Int, expected: T, actual: T) -> String)? = null) {
        // println("${TAG}：TestRunner.assertInTurnObserved()  thread -> ${Thread.currentThread().name}")

        val recordChanged = SynchronousQueue<T>()
        val recordCount = value.size
        thread {
            var seq = 0
            while (seq < recordCount) {
                val actual = recordChanged.poll(3, TimeUnit.SECONDS)
                val expected = value[seq++]
                // println("${TAG}：${testClassName}.${testMethodName}() 测试 -> [${seq}] $expected")

                val message = onFailedMessage?.invoke(seq, expected, actual)
                    ?:
"""

================$TAG================
测试类：$testClassName
测试方法：${testMethodName}()
$desc
测试失败：第${seq}个，预期值为[${expected}]，但是实际为[${actual}]
=====================================

""".trimIndent()
                assertEquals(expected, actual, message)
            }
        }


        if (this::_onLaunchActivity.isInitialized) {
            scenario.onActivity { activity ->
                _onBefore(activity)
                _onLaunchActivity.invoke(activity, Observer {
                    // println("${TAG}：${testClassName}.${testMethodName}() onChanged -> $it")
                    recordChanged.put(it)
                })

            }
        }

        if (::onActivityStopped.isInitialized) {
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity {
                onActivityStopped.invoke(it)
            }
        }

        if (::onActivityResumed.isInitialized) {
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity {
                onActivityResumed.invoke(it)
            }
        }
    }

}

