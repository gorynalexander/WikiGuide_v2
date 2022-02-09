package com.santo.wikiguide.data.repository

import com.santo.wikiguide.data.network.WikiApiService
import javax.inject.Inject
interface PlacesRepository{
    suspend fun loadPages()
}
class PlacesRepositoryImpl @Inject constructor(
    private val wikiApiService: WikiApiService
): PlacesRepository {
    override suspend fun loadPages(){
        wikiApiService.request("123", 10000, 128, 30)
    }
}