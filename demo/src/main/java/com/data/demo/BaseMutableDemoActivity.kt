package com.data.demo

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.MutableBackgroundLiveEvent
import androidx.lifecycle.MutableLiveEvent
import kotlinx.android.synthetic.main.activity_mutable.*
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


abstract class BaseMutableDemoActivity : AppCompatActivity() {
    abstract val liveEvent: MutableLiveEvent<String>
    abstract val backgroundLiveEvent: MutableBackgroundLiveEvent<String>
    private var b1 : ExecutorService?=null
    private val count = 5
    private val t1 = Executors.newSingleThreadExecutor()
    private val tm = Executors.newFixedThreadPool(count)

    private var backgroundOpen = false

    private val backgroundRunnable = Runnable {
        for (i in 100000..500000){
            if (!backgroundOpen) return@Runnable
            postValue("[间隔5s postValue $i]")
            try {
                Thread.sleep(5_000)
            }catch (e: InterruptedException) {
                return@Runnable
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_mutable)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        lifecycle.addObserver(LifecycleEventObserver{ _,event->
            logForLiveEvent("onStateChanged", event.name)
            logForBackgroundLiveEvent("onStateChanged", event.name)

        })

        setValue("init event")

        observeForMethod("observe")
//        observeForMethod("observeForever")

        initListener()
    }

    private fun initListener() {
        live_event_content.addOnLayoutChangeListener(View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            live_event_nsv.smoothScrollTo(0,bottom)
        })

        background_live_event_content.addOnLayoutChangeListener(View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            background_live_event_nsv.smoothScrollTo(0,bottom)
        })

        buttonClean.setOnClickListener {
            live_event_content.text = null
            background_live_event_content.text = null
        }

        backgOpen.setOnClickListener {
            if (backgroundOpen) {
                logForLiveEvent("fail [开启]间隔5s postValue")
                logForBackgroundLiveEvent("fail [开启]间隔5s postValue")
                return@setOnClickListener
            }
            logForLiveEvent("[开启]间隔5s postValue")
            logForBackgroundLiveEvent("[开启]间隔5s postValue")
            backgroundOpen = true
            b1 = Executors.newSingleThreadExecutor()
            b1?.execute(backgroundRunnable)
        }

        backgClose.setOnClickListener {
            if (!backgroundOpen) {
                logForLiveEvent("fail [关闭]间隔5s postValue")
                logForBackgroundLiveEvent("fail [关闭]间隔5s postValue")
                return@setOnClickListener
            }
            logForLiveEvent("[关闭]间隔5s postValue")
            logForBackgroundLiveEvent("[关闭]间隔5s postValue")
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
                set1.post {
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
            observeForMethod("observe")
        }
        observe1.setOnClickListener {
            removeObserveForMethod("observe")
        }

        observeForever.setOnClickListener {
            observeForMethod("observeForever")
        }
        observeForever1.setOnClickListener {
            removeObserveForMethod("observeForever")
        }
    }
    
    private fun setValue(value:String) {
        backgroundLiveEvent.setValue(value)
        liveEvent.setValue(value)
    }

    private fun postValue(value:String) {
        backgroundLiveEvent.postValue(value)
        liveEvent.postValue(value)
    }

    private val liveObservers = HashMap<String, Observer<String>>().apply {
        this["observe"] = Observer { logForLiveEvent("observe onChanged", it) }
        this["observeForever"] = Observer { logForLiveEvent("observeForever onChanged", it) }
    }
    
    private val backgroundLiveObservers = HashMap<String, Observer<String>>().apply {
        this["observe"] = Observer { logForBackgroundLiveEvent("observe onChanged ", it) }
        this["observeForever"] = Observer { logForBackgroundLiveEvent("observeForever onChanged", it) }
    }

    private fun observeForMethod(name:String){
        val liveObserver = liveObservers[name]!!
        val backgroundLiveObserver = backgroundLiveObservers[name]!!

        when (name) {
            "observe" -> {
                logForLiveEvent("添加观察", name)
                liveEvent.observe(this, liveObserver)
                logForBackgroundLiveEvent("添加观察", name)
                backgroundLiveEvent.observe(this, backgroundLiveObserver)
            }
            "observeForever" -> {
                logForLiveEvent("添加观察", name)
                liveEvent.observeForever(liveObserver)
                logForBackgroundLiveEvent("添加观察", name)
                backgroundLiveEvent.observeForever(backgroundLiveObserver)
            }
        }
    }

    private fun removeObserveForMethod(name:String){
        logForLiveEvent("移除观察", name)
        liveEvent.removeObserver(liveObservers[name]!!)

        logForBackgroundLiveEvent("移除观察", name)
        backgroundLiveEvent.removeObserver(backgroundLiveObservers[name]!!)
    }


    private fun logForBackgroundLiveEvent(tag:String, value:String? = null){
        val content = background_live_event_content
        val nsv = background_live_event_nsv
        log(content, nsv, tag, value, true)
    }

    private fun logForLiveEvent(tag:String, value:String? = null){
        val content = live_event_content
        val nsv = live_event_nsv
        log(content, nsv, tag, value, false)
    }

    private fun log(content:TextView, nsv: NestedScrollView, tag:String, value:String? = null, isBackground :Boolean){
        Log.e(
            "DEMO",
            "${if (isBackground) "BackgroundLiveEvent" else "LiveEvent"}    tag: $tag,  $value"
        )


        val dateFormat = SimpleDateFormat("HH:mm:ss.SSS")
        val currentTimeMillis = System.currentTimeMillis()
        val timeText = dateFormat.format(currentTimeMillis)
        content.post {
            content.append("\n${timeText} tag: $tag, $value\n")
        }
    }

    override fun onDestroy() {
        backgroundOpen = false
        super.onDestroy()
    }
}