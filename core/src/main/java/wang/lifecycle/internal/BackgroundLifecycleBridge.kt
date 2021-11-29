package wang.lifecycle.internal

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import wang.lifecycle.EventDispatcher
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal class BackgroundLifecycleBridge(private val dispatcher: InternalDispatcher) {

    private val sBackgroundLifecycleRegistry = ConcurrentHashMap<LifecycleOwner, LifecycleRegistry>()

    fun tryAttachBackgroundLifecycle(owner: LifecycleOwner) {
        if (sBackgroundLifecycleRegistry[owner] != null) return
        synchronized(this) {
            if (sBackgroundLifecycleRegistry[owner] != null) return
            sBackgroundLifecycleRegistry[owner] = attach(owner)
        }
    }

    fun getBackgroundLifecycle(owner: LifecycleOwner): LifecycleRegistry? {
        check(Thread.currentThread() == dispatcher.thread) {
            "Calling should on ${dispatcher.thread.name}, current ${Thread.currentThread().name}."
        }
        return sBackgroundLifecycleRegistry[owner]
    }

    private fun attach(owner: LifecycleOwner): LifecycleRegistry {
        return LifecycleRegistry(owner).also { lifecycleRegistry ->
            InternalReflect.closeCheckManiThreadOfLifecycleRegistry(lifecycleRegistry)

            val runnable = Runnable {
                if (owner.detachIfDestroy()) {
                    return@Runnable
                }

                val lifecycleEventObserver = object : LifecycleEventObserver {
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        if (event == Lifecycle.Event.ON_DESTROY) {
                            owner.lifecycle.removeObserver(this)
                        }

                        val targetState = event.targetState

                        // main -> dispatcher
                        dispatcher.dispatch(object :InternalRunnable{
                            override fun run() {
                                lifecycleRegistry.currentState = targetState

                                if (owner.detachIfDestroy()) {
                                    return
                                }
                            }
                        })

                    }
                }

                owner.lifecycle.addObserver(lifecycleEventObserver)
            }

            if (InternalMainExecutor.onMainThread) {
                runnable.run()
            } else {
                InternalMainExecutor.execute(runnable)
            }
        }
    }

    private fun LifecycleOwner.detachIfDestroy(): Boolean {
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            sBackgroundLifecycleRegistry.remove(this)
            return true
        }
        return false
    }
}