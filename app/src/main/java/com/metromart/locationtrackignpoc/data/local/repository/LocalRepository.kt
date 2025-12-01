package com.metromart.locationtrackignpoc.data.local.repository

import com.metromart.locationtrackignpoc.data.local.entity.LocationEntity

interface LocalRepository {
    suspend fun insertLocationData(data: LocationEntity)

    suspend fun getLocationData(): List<LocationEntity>
}
