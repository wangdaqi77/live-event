package com.data.demo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.data.demo.mixed.MixedBackgroundLiveEventDemoActivity
import com.data.demo.mixed.MixedLiveEventDemoActivity
import com.data.demo.other.SpecifiedThreadToObserveDemoActivity


class MainActivity : AppCompatActivity() {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }


    fun openLikeLiveDataDemo(view: View) {

        val intent =  Intent(this, LikeLiveDataDemoActivity::class.java)
        startActivity(intent)
    }

    fun openNoStickyLiveEventDemo(view: View) {

        val intent =  Intent(this, NoStickyDemoActivity::class.java)
        startActivity(intent)
    }

    fun openNoLossLiveEventDemo(view: View) {

        val intent =  Intent(this, NoLossDemoActivity::class.java)
        startActivity(intent)
    }

    fun openNoStickyNoLossLiveEventDemo(view: View) {

        val intent =  Intent(this, NoStickyNoLossDemoActivity::class.java)
        startActivity(intent)
    }

    fun openMixedLiveEventDemo(view: View) {
        val intent =  Intent(this, MixedLiveEventDemoActivity::class.java)
        startActivity(intent)
    }

    fun openMixedBackgroundLiveEventDemo(view: View) {
        val intent =  Intent(this, MixedBackgroundLiveEventDemoActivity::class.java)
        startActivity(intent)
    }

    fun openSpecifiedThreadToObserveEventDemo(view: View) {
        val intent =  Intent(this, SpecifiedThreadToObserveDemoActivity::class.java)
        startActivity(intent)
    }

}