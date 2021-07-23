package com.data.demo.other

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import wang.lifecycle.MutableBackgroundLiveEvent
import com.data.demo.R
import kotlinx.android.synthetic.main.activity_specified_thread_observe_event.*
import wang.lifecycle.BackgroundObserver
import wang.lifecycle.EventDispatcher
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory


class SpecifiedThreadToObserveDemoActivity : AppCompatActivity() {
    private var backgroundLiveEvent: MutableBackgroundLiveEvent<String> =
        MutableBackgroundLiveEvent()
    private var b1 : ExecutorService?=null
    private val count = 5
    private val t1 = Executors.newSingleThreadExecutor()
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
        log("onChanged 默认 - thread:${Thread.currentThread().name}", t)
    }

    private val observera =  object:
        BackgroundObserver<String>(EventDispatcher.MAIN){
        override fun onChanged(t: String) {
            log("onChanged EventDispatcher.MAIN - thread:${Thread.currentThread().name}", t)
        }
    }

    private val observerb =  object:
        BackgroundObserver<String>(EventDispatcher.BACKGROUND){
        override fun onChanged(t: String) {
            log("onChanged EventDispatcher.BACKGROUND - thread:${Thread.currentThread().name}", t)
        }
    }

    private val observerc =  object:
        BackgroundObserver<String>(EventDispatcher.ASYNC){
        override fun onChanged(t: String) {
            log("onChanged EventDispatcher.ASYNC - thread:${Thread.currentThread().name}", t)
        }
    }

    private val observerd = object : BackgroundObserver<String>(customEventDispatcher){
        override fun onChanged(t: String) {
            log("onChanged customEventDispatcher - thread:${Thread.currentThread().name}", t)
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


        log("setValue", "init event")
        setValue("init event")

        initListener()

        observe.performClick()
        observea.performClick()
        observeb.performClick()
        observec.performClick()
        observed.performClick()
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

        observe.setOnClickListener {
            log("添加观察器", "observe - 默认 - 默全局统一的子线程接收，不建议做耗时操作")
            backgroundLiveEvent.observe(this, observer)
        }
        observe1.setOnClickListener {
            log("移除观察器", "observe - 默认 - 默全局统一的子线程接收，不建议做耗时操作")
            backgroundLiveEvent.removeObserver(observer)
        }

        observea.setOnClickListener {
            log("添加观察器", "observe - EventDispatcher.MAIN - 主线程接收，不建议做耗时操作")
            backgroundLiveEvent.observe(this, observera)
        }
        observea1.setOnClickListener {
            log("移除观察器", "observe - EventDispatcher.MAIN - 主线程接收，不建议做耗时操作")
            backgroundLiveEvent.removeObserver(observera)
        }

        observeb.setOnClickListener {
            log("添加观察器", "observe - EventDispatcher.BACKGROUND - 后台线程接收，不建议做耗时操作")
            backgroundLiveEvent.observe(this, observerb)
        }
        observeb1.setOnClickListener {
            log("移除观察器", "observe - EventDispatcher.BACKGROUND - 后台线程接收，不建议做耗时操作")
            backgroundLiveEvent.removeObserver(observerb)
        }

        observec.setOnClickListener {
            log("添加观察器", "observe - EventDispatcher.ASYNC - 后台线程接收，可做耗时操作")
            backgroundLiveEvent.observe(this, observerc)
        }
        observec1.setOnClickListener {
            log("移除观察器", "observe - EventDispatcher.ASYNC - 后台线程接收，可做耗时操作")
            backgroundLiveEvent.removeObserver(observerc)
        }

        observed.setOnClickListener {
            log("添加观察器", "observe - customEventDispatcher - 单独指定自定义线程接收")
            backgroundLiveEvent.observe(this, observerd)
        }
        observed1.setOnClickListener {
            log("移除观察器", "observe - customEventDispatcher - 单独指定自定义线程接收")
            backgroundLiveEvent.removeObserver(observerd)
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

        Log.e("DEMO", "tag: $tag→      $value")

        val dateFormat = SimpleDateFormat("HH:mm:ss.SSS")
        val currentTimeMillis = System.currentTimeMillis()
        val timeText = dateFormat.format(currentTimeMillis)

        fullscreen_content.post {
            fullscreen_content.append(
                "\n${timeText} tag: $tag→ $value\n"
            )
        }


    }

    override fun onDestroy() {
        backgroundOpen = false
        super.onDestroy()
    }

    class CustomEventDispatcher: EventDispatcher {
        private val threadExecutor = Executors.newSingleThreadExecutor(object :ThreadFactory{
            override fun newThread(r: Runnable): Thread {
                return Thread(r, "单独指定的线程")
            }

        })
        override fun dispatch(runnable: Runnable) {
            threadExecutor.execute(runnable)
        }
    }

}