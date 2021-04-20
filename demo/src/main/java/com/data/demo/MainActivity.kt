package com.data.demo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity


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

    fun openLiveBackgroundEventDemo(view: View) {
        val intent =  Intent(this, LiveBackgroundEventDemoActivity::class.java)
        startActivity(intent)
    }

}