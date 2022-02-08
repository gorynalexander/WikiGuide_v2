package com.santo.wikiguide.data.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Thumbnail (
    @SerializedName("source")
    @Expose
    var source: String? = null,

    @SerializedName("width")
    @Expose
    var width: Int = 0,

    @SerializedName("height")
    @Expose
    var height: Int = 0
    )