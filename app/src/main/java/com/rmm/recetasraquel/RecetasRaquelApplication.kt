package com.rmm.recetasraquel

import android.app.Application
import com.rmm.recetasraquel.app.AppContainer

class RecetasRaquelApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
