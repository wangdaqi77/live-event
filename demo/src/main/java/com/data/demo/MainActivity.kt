package com.data.demo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.data.demo.other.SpecifiedThreadToObserveDemoActivity


class MainActivity : AppCompatActivity() {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun openLiveEventDemo(view: View) {
        val intent =  Intent(this, LiveEventDemoActivity::class.java)
        startActivity(intent)
    }

    fun openBackgroundLiveEventDemo(view: View) {
        val intent =  Intent(this, BackgroundLiveEventDemoActivity::class.java)
        startActivity(intent)
    }

    fun openSpecifiedThreadToObserveEventDemo(view: View) {
        val intent =  Intent(this, SpecifiedThreadToObserveDemoActivity::class.java)
        startActivity(intent)
    }

}