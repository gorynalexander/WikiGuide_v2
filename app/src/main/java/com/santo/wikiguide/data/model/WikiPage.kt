package com.santo.wikiguide.data.model

import androidx.annotation.Nullable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class WikiPage (
    @SerializedName("pageid")
    @Expose
    private val pageid: Int = 0,

    @SerializedName("ns")
    @Expose
    private val ns: Int = 0,

    @SerializedName("title")
    @Expose
    private val title: String? = null,

    @SerializedName("index")
    @Expose
    private val index: Int = 0,

    @SerializedName("coordinates")
    @Expose
    private val coordinates: List<Coordinate>? = null,

    @Nullable
    @SerializedName("thumbnail")
    @Expose
    private val thumbnail: Thumbnail? = null,

    @SerializedName("original")
    @Expose
    var original: Thumbnail? = null,

    @SerializedName("extract")
    @Expose
    private val extract: String? = null
)