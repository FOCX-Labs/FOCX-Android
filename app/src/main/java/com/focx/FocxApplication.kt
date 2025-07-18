package com.focx

import android.app.Application
import com.focx.utils.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FocxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.init(this)
    }
}