package com.santo.wikiguide.data.repository

import android.location.Location
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import javax.inject.Inject

interface LocationRepository {
    fun getUserLocationFlow(): Flow<Location>
    suspend fun sendLocation(location: Location)
}

class LocationRepositoryImpl @Inject constructor(): LocationRepository {
    private val activeLocation = ConflatedBroadcastChannel<Location>()

    override fun getUserLocationFlow(): Flow<Location> {
        return activeLocation.asFlow() // rework to new solution
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    override suspend fun sendLocation(location: Location) {
        activeLocation.send(location)
    }
}