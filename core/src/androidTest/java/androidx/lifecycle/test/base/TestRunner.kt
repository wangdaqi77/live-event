package androidx.lifecycle.test.base

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.test.core.app.ActivityScenario
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.assertEquals

class TestRunner<A : Activity, T>(
    private val scenario: ActivityScenario<A>,
    private val testClassName: String,
    private val testMethodName: String
) {
    companion object {
        private const val TAG = "TestRunner"

        fun <A : Activity, T> newRunner(
            scenario: ActivityScenario<A>,
            testClassName: String,
            testMethodName: String
        ) = TestRunner<A, T>(
            scenario,
            testClassName,
            testMethodName
        )
    }

    private lateinit var onActivityRun: (activity: A, Observer<T>) -> Unit
    private lateinit var onActivityRunForNestedCallSetValue: (activity: A, Observer<T>, Observer<T>) -> Unit
    private lateinit var onActivityStopped: (activity: A) -> Unit
    private lateinit var onActivityStarted: (activity: A) -> Unit
    private lateinit var onActivityDestroy: (activity: A) -> Unit
    private var onChanged2: (value: T) -> Unit = {}

    fun runActivity(block: (activity: A, Observer<T>) -> Unit): TestRunner<A, T> {
        this.onActivityRun = block
        return this
    }
    fun runActivityForNestedCallSetValue(block: (activity: A, Observer<T>, Observer<T>) -> Unit): TestRunner<A, T> {
        this.onActivityRunForNestedCallSetValue = block
        return this
    }

    fun stoppedActivity(block: (activity: A) -> Unit): TestRunner<A, T> {
        this.onActivityStopped = block
        return this
    }

    fun startedActivity(block: (activity: A) -> Unit): TestRunner<A, T> {
        this.onActivityStarted = block
        return this
    }

    fun destroyActivity(block: (activity: A) -> Unit): TestRunner<A, T> {
        this.onActivityDestroy = block
        return this
    }

    fun onChangedOfObserver1(block: (value: T) -> Unit): TestRunner<A, T> {
        this.onChanged2 = block
        return this
    }

    fun startAssertChangedOrder(vararg value: T, onFailedMessage: ((seq: Int, expected: T, actual: T) -> String)? = null) {
        println("${TAG}：TestRunner.startAssertOrder()  thread -> ${Thread.currentThread().name}")

        val recordChanged = SynchronousQueue<T>()
        val recordCount = value.size
        thread {
            var seq = 0
            while (seq < recordCount) {
                val actual = recordChanged.poll(3, TimeUnit.SECONDS)
                val expected = value[seq++]
                println("${TAG}：${testClassName}.${testMethodName}() 测试 -> [${seq}] $expected")

                val message = onFailedMessage?.invoke(seq, expected, actual) ?: "${TAG}：${testClassName}.${testMethodName}() 测试失败：第${seq}个，预期值为${expected}，但是实际为${actual}"
                assertEquals(expected, actual, message)
            }
        }


        if (::onActivityRun.isInitialized) {
            scenario.onActivity { activity ->
                onActivityRun.invoke(activity, Observer {
                    println("${TAG}：${testClassName}.${testMethodName}() onChanged -> $it")
                    recordChanged.put(it)
                })

            }
        }


        if (::onActivityRunForNestedCallSetValue.isInitialized) {
            scenario.onActivity { activity ->
                onActivityRunForNestedCallSetValue.invoke(
                    activity,
                    Observer {
                        println("${TAG}：${testClassName}.${testMethodName}() onChanged1 -> $it")
                        onChanged2(it)
                    },
                    Observer {
                        println("${TAG}：${testClassName}.${testMethodName}() onChanged2 -> $it")
                        recordChanged.put(it)
                    }
                )

            }
        }

        if (::onActivityStopped.isInitialized) {
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity {
                onActivityStopped.invoke(it)
            }
        }

        if (::onActivityStarted.isInitialized) {
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity {
                onActivityStarted.invoke(it)
            }
        }
    }

}

