package com.openjlpt.app

import android.app.Application
import com.openjlpt.app.data.AppContainer

class OpenJlptApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
