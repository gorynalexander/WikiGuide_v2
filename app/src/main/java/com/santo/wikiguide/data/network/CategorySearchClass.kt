package com.santo.wikiguide.data.network


import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.mapbox.geojson.BoundingBox
import com.mapbox.geojson.Point
import com.mapbox.search.CategorySearchEngine
import com.mapbox.search.CategorySearchOptions
import com.mapbox.search.MapboxSearchSdk
import com.mapbox.search.ResponseInfo
import com.mapbox.search.SearchCallback
import com.mapbox.search.SearchRequestTask
import com.mapbox.search.result.SearchResult
import timber.log.Timber

class CategorySearchClass() {

    private var categorySearchEngine: CategorySearchEngine
    private lateinit var searchRequestTask: SearchRequestTask

    init {
        categorySearchEngine = MapboxSearchSdk.getCategorySearchEngine()
    }


    fun getPlacesByCategory(
        category_name: String,
        limit: Int,
        currentLocation: Point,
        resultListener:SearchResultListener
    ){
        Timber.i("Begin search")

        val searchCallback: SearchCallback = object : SearchCallback {

            override fun onResults(results: List<SearchResult>, responseInfo: ResponseInfo) {
                if (results.isEmpty()) {
                    Timber.i("No category search results")
                } else {
                    resultListener.onSearchResult(results)
                    Timber.i("Category search results: " + results)
                }
            }
            override fun onError(e: Exception) {
                Timber.i(e, "Search error")
            }
        }

        searchRequestTask = categorySearchEngine.search(
            category_name,
            CategorySearchOptions(limit = limit),
            searchCallback
        )
    }
    fun interface SearchResultListener{
        fun onSearchResult(results:List<SearchResult>)
    }

// Will it work? Should work like onDestroy()
    fun finalize(){
        searchRequestTask.cancel()
    }
}