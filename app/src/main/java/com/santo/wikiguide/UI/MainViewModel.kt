package com.santo.wikiguide.UI

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santo.wikiguide.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {
    fun sendLocation(location: Location) {
        viewModelScope.launch {
            locationRepository.sendLocation(location)
        }
    }
}