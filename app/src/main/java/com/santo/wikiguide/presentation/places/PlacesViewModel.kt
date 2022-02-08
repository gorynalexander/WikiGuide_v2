package com.santo.wikiguide.presentation.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santo.wikiguide.data.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlacesViewModel @Inject constructor(
    val placesRepository: PlacesRepository
): ViewModel() {
    fun loadPlaces() {
        viewModelScope.launch(Dispatchers.IO) {
            placesRepository.loadPages()
        }
    }
}