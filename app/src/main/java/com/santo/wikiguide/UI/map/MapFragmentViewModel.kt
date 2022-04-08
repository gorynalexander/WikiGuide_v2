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
import com.santo.wikiguide.data.routerBuilder.RouteBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.coroutineContext


@HiltViewModel
class MapFragmentViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val locationRepository: LocationRepository): ViewModel() {

    lateinit var currentLocation: Point
    private val categorySearchClass: CategorySearchClass= CategorySearchClass()
    private val reverseGeocodingClass: ReverseGeocodingClass =ReverseGeocodingClass()
    private val _poiList= MutableLiveData<List<SearchResult>>()

    init {
        _poiList.value= ArrayList<SearchResult>()
    }
    val poiList: LiveData<List<SearchResult>>
        get() = _poiList

    fun getPOIs(category_name:String,limit:Int){
        categorySearchClass.getPlacesByCategory(category_name =category_name,
            limit = limit,
            currentLocation = currentLocation){ result->
            _poiList.value=_poiList.value?.plus(result)
//            _poiList.value=result
//           TODO: CHECK IF NO RESULT!
        }
    }
    fun getReverseGeocodingResult(point: Point){
        reverseGeocodingClass.getReverseGeocodingResult(point)
    }


}