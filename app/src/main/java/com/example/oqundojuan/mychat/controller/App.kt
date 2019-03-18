package com.example.oqundojuan.mychat.controller

import android.app.Application
import com.example.oqundojuan.mychat.Utilities.SharedPrefs

class App: Application() {

    companion object {
        lateinit var prefs: SharedPrefs
    }
    override fun onCreate() {
        prefs = SharedPrefs(applicationContext)
        super.onCreate()
    }
}