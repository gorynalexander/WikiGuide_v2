package com.santo.wikiguide.UI.places

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santo.wikiguide.data.repository.LocationRepository
import com.santo.wikiguide.data.repository.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlacesViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val locationRepository: LocationRepository
): ViewModel() {
    fun loadPlaces() {
        viewModelScope.launch(Dispatchers.IO) {
            placesRepository.loadPages()
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    fun locationFlow(): Flow<Location> {
        return locationRepository.getUserLocationFlow()
    }
}