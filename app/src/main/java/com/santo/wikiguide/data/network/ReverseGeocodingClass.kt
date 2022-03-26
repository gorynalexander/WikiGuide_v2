package com.santo.wikiguide.data.network

import android.os.Bundle
import android.util.Log
import com.mapbox.geojson.Point
import com.mapbox.search.*
import com.mapbox.search.result.SearchResult
import timber.log.Timber

class ReverseGeocodingClass() {


    private  var reverseGeocoding: ReverseGeocodingSearchEngine
    private lateinit var searchRequestTask: SearchRequestTask

    init {
        reverseGeocoding = MapboxSearchSdk.getReverseGeocodingSearchEngine()
    }

    private val searchCallback = object : SearchCallback {

        override fun onResults(results: List<SearchResult>, responseInfo: ResponseInfo) {
            if (results.isEmpty()) {
                Timber.i("No reverse geocoding results")
            } else {
                Timber.i("Reverse geocoding results: " + results)
            }
        }

        override fun onError(e: Exception) {
            Timber.i(e, "Reverse geocoding error")
            return
        }
    }
    fun getReverseGeocodingResult(point:Point){
        val options = ReverseGeoOptions(
            center = point,
            limit = 1
        )
        searchRequestTask = reverseGeocoding.search(options, searchCallback)
    }

// Will it work? Should work like onDestroy()
    fun finalize() {
        searchRequestTask.cancel()
    }
}