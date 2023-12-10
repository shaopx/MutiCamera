package com.spx.muticamera

import android.app.Application
import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

class TmfApplication : Application() {

    companion object {
        private lateinit var instance: TmfApplication
        var screenWidth: Int = 0
        var screenHeight: Int = 0

        @JvmStatic
        fun getContext(): Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 获取屏幕宽度和高度
        val displayMetrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }
}