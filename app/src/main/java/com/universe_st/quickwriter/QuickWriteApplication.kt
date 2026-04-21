package com.universe_st.quickwriter

import android.app.Application
import com.universe_st.quickwriter.di.AppContainer

class QuickWriteApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}