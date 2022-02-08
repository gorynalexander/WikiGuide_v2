package com.santo.wikiguide.data.model

import androidx.annotation.Nullable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class WikiQuery (
    @SerializedName("pages")
    @Expose
    @Nullable
    private val pages: List<WikiPage>? = null
)