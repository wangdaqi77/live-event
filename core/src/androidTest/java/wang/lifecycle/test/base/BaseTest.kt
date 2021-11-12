package wang.lifecycle.test.base

import androidx.activity.ComponentActivity
import androidx.annotation.CallSuper
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import org.junit.After
import org.junit.Before

abstract class BaseTest{
    companion object {
        const val EVENT_INIT = "EVENT_INIT"
    }

    lateinit var scenario: ActivityScenario<ComponentActivity>

    fun newObserveTestRunner(methodName: String, desc: String? = null) =
        ObserveTestRunner.newRunner<ComponentActivity, String>(
            scenario,
            this.javaClass.simpleName,
            methodName,
            desc
        )

    @CallSuper
    @Before
    open fun setUp() {
        scenario = launchActivity()
    }

    @CallSuper
    @After
    open fun tearDown() {
        scenario.close()
    }

}