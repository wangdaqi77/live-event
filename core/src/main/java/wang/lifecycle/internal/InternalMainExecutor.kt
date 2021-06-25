package wang.lifecycle.internal

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

internal object InternalMainExecutor : Executor{

    private val mMainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

    val isMainThread : Boolean
        get() = Thread.currentThread() == Looper.getMainLooper().thread

    override fun execute(command: Runnable) {
        mMainHandler.post(command)
    }

}