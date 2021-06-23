package androidx.lifecycle.test.base

import androidx.activity.ComponentActivity
import androidx.annotation.CallSuper
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import org.junit.After
import org.junit.Before

abstract class BaseTest{
    companion object {
        const val EVENT_INIT = "initEvent"
    }

    lateinit var scenario: ActivityScenario<ComponentActivity>

    fun newTestRunner(methodName: String) =
        TestRunner.newRunner<ComponentActivity, String>(
            scenario,
            this.javaClass.simpleName,
            methodName
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