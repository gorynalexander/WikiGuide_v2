package com.santo.wikiguide.data.model

class Excursion(
    private val title: String? = null,
    private val excursionPlaces: List<ExcursionPlace> = ArrayList(),
    private val distance: Double = 0.0
)
