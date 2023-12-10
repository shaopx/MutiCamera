package com.spx.muticamera

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.spx.muticamera.ext.hideSystemUI

class MainActivity : AppCompatActivity() {
    private val TAG = "tmf_MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = window
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        setContentView(R.layout.activity_main)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
//        if (hasFocus) {
//            hideSystemUI()
//        }
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause: ...")
        // 为了避免资源泄露和摄像头硬件问题, 暂时处理逻辑为整体退出
        finish()
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume: ...")
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "onStop: ...")

    }
}