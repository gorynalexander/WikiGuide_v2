package com.santo.wikiguide.data.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Coordinate (
    @SerializedName("lat")
    @Expose
    private var lat: Float = 0f,

    @SerializedName("lon")
    @Expose
    private var lon: Float = 0f,

    @SerializedName("primary")
    @Expose
    private var primary: String? = null,

    @SerializedName("globe")
    @Expose
    private var globe: String? = null
)