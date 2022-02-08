package com.santo.wikiguide.data.model

class ExcursionPlace(
    private var placeTitle: String? = "",
    private val thumbURL: String = "",
    private val lat: Double = 0.0,
    private val lon: Double = 0.0,
    private val distance: Double = 0.0,
    private val description: String? = null
)
