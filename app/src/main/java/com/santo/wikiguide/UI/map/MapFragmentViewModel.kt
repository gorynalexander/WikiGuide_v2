package com.santo.wikiguide.UI.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.geojson.Point
import com.mapbox.search.result.SearchResult
import com.santo.wikiguide.data.network.CategorySearchClass
import com.santo.wikiguide.data.network.ReverseGeocodingClass
import com.santo.wikiguide.data.repository.LocationRepository
import com.santo.wikiguide.data.repository.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class MapFragmentViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val locationRepository: LocationRepository): ViewModel() {

    private val categorySearchClass: CategorySearchClass= CategorySearchClass()
    private val reverseGeocodingClass: ReverseGeocodingClass =ReverseGeocodingClass()

    private val _poiList= MutableLiveData<List<SearchResult>>()
    val poiList: LiveData<List<SearchResult>>
    get() = _poiList

    fun getPOIs(category_name:String,limit:Int){
        categorySearchClass.getPlacesByCategory(category_name =category_name, limit = limit){ result->
            _poiList.value=result
        }
    }
    fun getReverseGeocodingResult(point: Point){
        reverseGeocodingClass.getReverseGeocodingResult(point)
    }
}