package wang.lifecycle.internal

import android.os.Handler
import android.os.HandlerThread
import wang.lifecycle.EventDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

internal class InternalDispatcher internal constructor(name: String) : EventDispatcher {
    internal val thread = HandlerThread(name)
    private val handler: Handler by lazy { Handler(thread.looper) }

    init {
        thread.start()
    }

    override fun dispatch(runnable: Runnable) {
        check(runnable is InternalRunnable) { "Not call dispatch!" }
        if (Thread.currentThread() !== thread) {
            handler.post(runnable)
            return
        }
        runnable.run()
    }

    internal fun dispatch(block: () -> Unit) {
        dispatch(object : InternalRunnable {
            override fun run() {
                block()
            }
        })
    }
}

internal class InternalMainDispatcher internal constructor() : EventDispatcher {
    override fun dispatch(runnable: Runnable) {
        check(runnable is InternalRunnable) { "Not call dispatch!" }
        InternalMainExecutor.execute(runnable)
    }
}

internal class InternalAsyncDispatcher internal constructor() : EventDispatcher {
    private val group: ThreadGroup
    private val namePrefix: String
    private val threadNumber = AtomicInteger(1)

    init {
        val var1 = System.getSecurityManager()
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        this.group = if (var1 != null) var1.threadGroup else Thread.currentThread().threadGroup
        this.namePrefix = "pool-async-event-dispatcher-thread-"
    }

    private val executor by lazy {
        Executors.newCachedThreadPool{
            Thread(group, it, namePrefix + threadNumber.getAndIncrement(), 0L)
                .apply {
                    if (isDaemon) isDaemon = false
                    if (priority != 5) priority = 5
                }
        }
    }

    override fun dispatch(runnable: Runnable) {
        check(runnable is InternalRunnable) { "Not call dispatch!" }
        executor.execute(runnable)
    }
}