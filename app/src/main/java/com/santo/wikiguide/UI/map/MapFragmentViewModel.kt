package com.santo.wikiguide.UI.map

import androidx.lifecycle.ViewModel
import com.santo.wikiguide.data.network.SearchClass
import com.santo.wikiguide.data.repository.LocationRepository
import com.santo.wikiguide.data.repository.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class MapFragmentViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val locationRepository: LocationRepository
): ViewModel() {
    private val searchClass=SearchClass()

    fun getPOIs(){
        searchClass.getPlacesByCategory()
    }
}