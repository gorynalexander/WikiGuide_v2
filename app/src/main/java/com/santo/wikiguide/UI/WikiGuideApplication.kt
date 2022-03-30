package com.santo.wikiguide.UI

import android.app.Application
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.search.MapboxSearchSdk
import com.santo.wikiguide.BuildConfig
import com.santo.wikiguide.R
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
        MapboxSearchSdk.initialize(
            application = this,
            accessToken = getString(R.string.mapbox_access_token),
            locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        )
    }
}

//TODO
//CategorySearchOptions( current location pass,limit = 2),
//Custom user's marker. Rotation of the map