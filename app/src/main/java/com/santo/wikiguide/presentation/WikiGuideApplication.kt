package com.santo.wikiguide.presentation

import android.app.Application
import com.santo.wikiguide.BuildConfig
import com.santo.wikiguide.util.log.TimberTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class WikiGuideApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(TimberTree()) // init logger for logcat
        }
    }
}