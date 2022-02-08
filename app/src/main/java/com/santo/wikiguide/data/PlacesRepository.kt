package com.santo.wikiguide.data

import com.santo.wikiguide.data.network.WikiApiService
import javax.inject.Inject

class PlacesRepository @Inject constructor(
    private val wikiApiService: WikiApiService
) {
    suspend fun loadPages(){
        wikiApiService.request("123", 10000, 128, 30)
    }
}