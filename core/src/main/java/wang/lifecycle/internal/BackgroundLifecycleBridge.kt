package wang.lifecycle.internal

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import wang.lifecycle.EventDispatcher
import java.util.*
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

internal class BackgroundLifecycleBridge(private val dispatcher: EventDispatcher) {

    private val sBackgroundLifecycleRegistry = WeakHashMap<LifecycleOwner, LifecycleRegistry>()
    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private val writeLock = lock.writeLock()

    fun tryAttachBackgroundLifecycle(owner: LifecycleOwner) {
        if (sBackgroundLifecycleRegistry[owner] != null) return
        writeLock.lock()
        try {
            if (sBackgroundLifecycleRegistry[owner] != null) return
            sBackgroundLifecycleRegistry[owner] = attach(owner)
        } finally {
            writeLock.unlock()
        }
    }

    fun getBackgroundLifecycle(owner: LifecycleOwner) = sBackgroundLifecycleRegistry[owner]

    private fun attach(owner: LifecycleOwner): LifecycleRegistry {
        return LifecycleRegistry(owner).apply {
            // LifecycleRegistry - main -> dispatcher
            InternalReflect.closeCheckManiThreadOfLifecycleRegistry(this)
            val runnable = Runnable {
                val lifecycleEventObserver = object : LifecycleEventObserver {
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        if (event == Lifecycle.Event.ON_DESTROY) {
                            owner.lifecycle.removeObserver(this)
                        }
                        val targetState = event.targetState
                        dispatcher.dispatch(object :InternalRunnable{
                            override fun run() {
                                this@apply.currentState = targetState
                            }
                        })
                    }
                }

                owner.lifecycle.addObserver(lifecycleEventObserver)
            }

            if (InternalMainExecutor.isMainThread) {
                runnable.run()
            } else {
                InternalMainExecutor.execute(runnable)
            }
        }
    }
}