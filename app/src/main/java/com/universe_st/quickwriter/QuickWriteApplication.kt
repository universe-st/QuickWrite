package com.universe_st.quickwriter

import android.app.Application
import com.universe_st.quickwriter.di.AppContainer
import timber.log.Timber


class QuickWriteApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if(BuildConfig.DEBUG){
            Timber.plant(Timber.DebugTree())
        }
        appContainer = AppContainer(this)
    }
}