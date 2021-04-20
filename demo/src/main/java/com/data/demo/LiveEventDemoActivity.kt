package com.data.demo

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.wrapper.LiveEvent
import androidx.lifecycle.wrapper.MutableLiveEvent
import kotlinx.android.synthetic.main.activity_full.*
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class LiveEventDemoActivity : AppCompatActivity() {
    private var liveEvent: MutableLiveEvent<String> = MutableLiveEvent()
    private var b1 : ExecutorService?=null
    private val count = 5
    private val t1 = Executors.newSingleThreadExecutor()
    private val tm = Executors.newFixedThreadPool(count)

    private var backgroundOpen = false

    private val backgroundRunnable = Runnable {
        for (i in 100000..500000){
            // if (!backgroundOpen) return@Runnable
            postValue("[后台 $i]")
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

        setContentView(R.layout.activity_full)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        lifecycle.addObserver(LifecycleEventObserver{ _,event->
            log("onStateChanged", event.name)

        })

//        val a = Observer<String> { log("onChanged", it) }
//        fullLiveData.observeNoStickyNoLoss(this, a)
//        fullLiveData.observeForeverNoStickyNoLoss(a)
//        fullLiveData.observeForeverNoLoss( a)
//        fullLiveData.observeNoSticky(this, a)


        setValue("init value")

        observeForMethod("observe")
        observeForMethod("observeNoSticky")
        observeForMethod("observeNoLoss")

        initListener()
    }

    private fun initListener() {

        buttonClean.setOnClickListener {
            fullscreen_content.text = null
        }

        backgOpen.setOnClickListener {
            if (backgroundOpen) {
                log("fail 开启后台post(间隔5s)")
                return@setOnClickListener
            }
            log("开启后台post(间隔5s)")
            backgroundOpen = true
            b1 = Executors.newSingleThreadExecutor()
            b1?.execute(backgroundRunnable)
        }

        backgClose.setOnClickListener {
            if (!backgroundOpen) {
                log("fail 关闭后台post")
                return@setOnClickListener
            }
            log("关闭后台post")
            backgroundOpen = false
            b1?.shutdownNow()
        }

        post1.setOnClickListener {
            t1.execute {
                postValue("[post]")
            }
        }

        postM.setOnClickListener {
            for (i in 1..count) {
                tm.execute {
                    postValue("[post][$i]")
                }
            }

        }
        set1.setOnClickListener {
            t1.execute {
                fullscreen_content.post {
                    setValue("[set]")
                }

            }
        }

        setM.setOnClickListener {
            for (i in 1..count) {
                tm.execute {
                    fullscreen_content.post {
                        setValue("[set][$i]")
                    }
                }
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

        observeNoLoss.setOnClickListener { observeForMethod("observeNoLoss") }
        observeNoLoss1.setOnClickListener { removeObserveForMethod("observeNoLoss") }

        observeForeverNoLoss.setOnClickListener { observeForMethod("observeForeverNoLoss") }
        observeForeverNoLoss1.setOnClickListener { removeObserveForMethod("observeForeverNoLoss") }

        observeNoSticky.setOnClickListener { observeForMethod("observeNoSticky") }
        observeNoSticky1.setOnClickListener { removeObserveForMethod("observeNoSticky") }

        observeForeverNoSticky.setOnClickListener { observeForMethod("observeForeverNoSticky") }
        observeForeverNoSticky1.setOnClickListener { removeObserveForMethod("observeForeverNoSticky") }

        observeNoStickyNoLoss.setOnClickListener { observeForMethod("observeNoStickyNoLoss") }
        observeNoStickyNoLoss1.setOnClickListener { removeObserveForMethod("observeNoStickyNoLoss") }

        observeForeverNoStickyNoLoss.setOnClickListener { observeForMethod("observeForeverNoStickyNoLoss") }
        observeForeverNoStickyNoLoss1.setOnClickListener { removeObserveForMethod("observeForeverNoStickyNoLoss") }


    }

    override fun onStop() {
        super.onStop()
    }

    private fun setValue(value:String) {
        liveEvent.setValue(value)
    }

    private fun postValue(value:String) {
        liveEvent.postValue(value)
    }

    private val observers = HashMap<String, Observer<String>>().apply {
        this["observe"] = Observer { log("observe onChanged", it) }
        this["observeForever"] = Observer { log("observeForever onChanged", it) }
        this["observeNoSticky"] = Observer { log("observeNoSticky onChanged", it) }
        this["observeForeverNoSticky"] = Observer { log("observeForeverNoSticky onChanged", it) }
        this["observeNoLoss"] = Observer { log("observeNoLoss onChanged", it) }
        this["observeForeverNoLoss"] = Observer { log("observeForeverNoLoss onChanged", it) }
        this["observeNoStickyNoLoss"] = Observer { log("observeNoStickyNoLoss onChanged", it) }
        this["observeForeverNoStickyNoLoss"] = Observer { log("observeForeverNoStickyNoLoss onChanged", it) }
    }

    private fun observeForMethod(name:String){
        val observer = observers[name]!!
        log("添加观察", name)

        when (name) {
            "observe" -> { liveEvent.observe(this, observer) }
            "observeForever" -> { liveEvent.observeForever(observer) }
            "observeNoSticky" -> liveEvent.observeNoSticky(this, observer)
            "observeForeverNoSticky" -> liveEvent.observeForeverNoSticky(observer)
            "observeNoLoss" -> { liveEvent.observeNoLoss(this, observer) }
            "observeForeverNoLoss" -> { liveEvent.observeForeverNoLoss(observer) }
            "observeNoStickyNoLoss" -> liveEvent.observeNoStickyNoLoss(this, observer)
            "observeForeverNoStickyNoLoss" -> liveEvent.observeForeverNoStickyNoLoss(observer)
        }
    }

    private fun removeObserveForMethod(name:String){
        log("移除观察", name)
        liveEvent.removeObserver(observers[name]!!)
    }


    private fun log(tag:String, value:String? = null){

        Log.e("DEMO", "tag: $tag,      $value")

        val dateFormat = SimpleDateFormat("HH:mm:ss.SSS")
        val currentTimeMillis = System.currentTimeMillis()
        val timeText = dateFormat.format(currentTimeMillis)

        fullscreen_content.post {
            val child = nsv.getChildAt(0)
            val lp = child.getLayoutParams() as ViewGroup.MarginLayoutParams
            val parentSpace: Int = nsv.getHeight() - nsv.getPaddingTop() - nsv.getPaddingBottom()
            val scrollY: Int = nsv.getScrollY()
            var childSize: Int = child.getHeight() + lp.topMargin + lp.bottomMargin
            var maxY = Math.max(0, childSize - parentSpace)

            var scrollBottom = false
            if (scrollY == maxY) {
                scrollBottom = true
            }

            fullscreen_content.append(
                "\n${timeText} tag: $tag, $value\n"
            )

            if (scrollBottom) {
                fullscreen_content.post {
                    childSize = child.getHeight() + lp.topMargin + lp.bottomMargin
                    maxY = Math.max(0, childSize - parentSpace)
                    nsv.smoothScrollTo(0, maxY)
                }
            }
        }


    }

    override fun onDestroy() {
        super.onDestroy()
    }

}