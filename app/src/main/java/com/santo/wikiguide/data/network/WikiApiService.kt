package com.santo.wikiguide.data.network

import com.santo.wikiguide.data.model.QueryResult
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WikiApiService {
    @GET("w/api.php?action=query&format=json&prop=coordinates|pageimages|pageterms&generator=geosearch&formatversion=2&colimit=50&piprop=thumbnail|original&pilimit=50&wbptterms=description")
    suspend fun request(
        @Query("ggscoord") coord: String,
        @Query("ggsradius") radius: Int,
        @Query("pithumbsize") thumbsize: Int,
        @Query("ggslimit") ggslimit: Int
    ): QueryResult?
}