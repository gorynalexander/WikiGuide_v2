package com.santo.wikiguide.data.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class QueryResult (
    @SerializedName("batchcomplete")
    @Expose
    private val batchcomplete: Boolean = false,

    @SerializedName("query")
    @Expose
    private val query: WikiQuery? = null
)