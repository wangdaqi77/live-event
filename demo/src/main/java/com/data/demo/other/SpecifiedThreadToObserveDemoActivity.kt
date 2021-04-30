package com.data.demo.other

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import androidx.lifecycle.mutable.MixedBackgroundLiveEvent
import com.data.demo.R
import kotlinx.android.synthetic.main.activity_specified_thread_observe_event.*
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory


class SpecifiedThreadToObserveDemoActivity : AppCompatActivity() {
    private var backgroundLiveEvent: MixedBackgroundLiveEvent<String> =
        MixedBackgroundLiveEvent()
    private var b1 : ExecutorService?=null
    private val count = 5
    private val t1 = Executors.newSingleThreadExecutor()
    private val tm = Executors.newFixedThreadPool(count)
    private val customGlobalEventDispatcher = CustomGlobalEventDispatcher() // 全局的分发器
    private val customEventDispatcher = CustomEventDispatcher() // 单个Observer指定的分发器
    private var backgroundOpen = false

    private val backgroundRunnable = Runnable {
        for (i in 100000..500000){
             if (!backgroundOpen) return@Runnable
            postValue("[后台 $i]")
            try {
                Thread.sleep(5_000)
            }catch (e: InterruptedException) {
                return@Runnable
            }
        }
    }

    private val observer =  Observer<String>{ t ->
        log("onChanged thread:${Thread.currentThread().name}", t)
    }

    private val specifiedThreadBackgroundObserver = object : BackgroundObserver<String>(customEventDispatcher){
        override fun onChanged(t: String) {
            log("onChanged thread:${Thread.currentThread().name}", t)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_specified_thread_observe_event)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lifecycle.addObserver(LifecycleEventObserver{ _,event->
            log("onStateChanged", event.name)
        })


        setValue("init event")

        initListener()

        addGlobalDispatcher.performClick()
        observe.performClick()
        observec.performClick()
    }

    private fun initListener() {

        fullscreen_content.addOnLayoutChangeListener(View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            nsv.smoothScrollTo(0,bottom)
        })

        buttonClean.setOnClickListener {
            fullscreen_content.text = null
        }

        backgOpen.setOnClickListener {
            if (backgroundOpen) {
                log("fail [开启]间隔5s postValue")
                return@setOnClickListener
            }
            log("[开启]间隔5s postValue")
            backgroundOpen = true
            b1 = Executors.newSingleThreadExecutor()
            b1?.execute(backgroundRunnable)
        }

        backgClose.setOnClickListener {
            if (!backgroundOpen) {
                log("fail [关闭]间隔5s postValue")
                return@setOnClickListener
            }
            log("[关闭]间隔5s postValue")
            backgroundOpen = false
            b1?.shutdownNow()
        }

        post1.setOnClickListener {
            t1.execute {
                postValue("[postValue]")
            }
        }

        postM.setOnClickListener {
            t1.execute {
                for (i in 1..count) {
                    postValue("[postValue][$i]")
                }
            }
        }
        set1.setOnClickListener {
            t1.execute {
                fullscreen_content.post {
                    setValue("[setValue]")
                }

            }
        }

        setM.setOnClickListener {
            for (i in 1..count) {
                setValue("[setValue][$i]")
            }
        }


        addGlobalDispatcher.setOnClickListener {

            log("设置全局事件分发器（指定全局线程分发事件）", "setCustomGlobalBackgroundEventDispatcher")
            BackgroundLiveEvent.setCustomGlobalBackgroundEventDispatcher(customGlobalEventDispatcher)
        }
        removeGlobalDispatcher.setOnClickListener {
            log("移除全局事件分发器", "removeCustomGlobalBackgroundEventDispatcher")
            BackgroundLiveEvent.removeCustomGlobalBackgroundEventDispatcher()
        }

        observe.setOnClickListener {
            log("添加观察器", "observe")
            backgroundLiveEvent.observe(this, observer)
        }
        observe1.setOnClickListener {
            log("移除观察器", "observe")
            backgroundLiveEvent.removeObserver(observer)
        }

        observec.setOnClickListener {
            log("添加观察器BackgroundObserver-自定义分发器（指定线程）", "observe")
            backgroundLiveEvent.observe(this, specifiedThreadBackgroundObserver)
        }
        observec1.setOnClickListener {
            log("移除观察器BackgroundObserver", "observe")
            backgroundLiveEvent.removeObserver(specifiedThreadBackgroundObserver)
        }

    }

    override fun onStop() {
        super.onStop()
    }

    private fun setValue(value:String) {
        backgroundLiveEvent.setValue(value)
    }

    private fun postValue(value:String) {
        backgroundLiveEvent.postValue(value)
    }

    private fun log(tag:String, value:String? = null){

        Log.e("DEMO", "tag: $tag,      $value")

        val dateFormat = SimpleDateFormat("HH:mm:ss.SSS")
        val currentTimeMillis = System.currentTimeMillis()
        val timeText = dateFormat.format(currentTimeMillis)

        fullscreen_content.post {
            fullscreen_content.append(
                "\n${timeText} tag: $tag, $value\n"
            )
        }


    }

    override fun onDestroy() {
        backgroundOpen = false
        BackgroundLiveEvent.removeCustomGlobalBackgroundEventDispatcher()
        super.onDestroy()
    }

    class CustomGlobalEventDispatcher: EventDispatcher {
        private val threadExecutor = Executors.newSingleThreadExecutor(object :ThreadFactory{
            override fun newThread(r: Runnable): Thread {
                return Thread(r, "指定的全局线程")
            }

        })
        override fun dispatch(runnable: Runnable) {
            threadExecutor.execute(runnable)
        }
    }

    class CustomEventDispatcher: EventDispatcher {
        private val threadExecutor = Executors.newSingleThreadExecutor(object :ThreadFactory{
            override fun newThread(r: Runnable): Thread {
                return Thread(r, "BackgroundObserver指定的线程")
            }

        })
        override fun dispatch(runnable: Runnable) {
            threadExecutor.execute(runnable)
        }
    }

}