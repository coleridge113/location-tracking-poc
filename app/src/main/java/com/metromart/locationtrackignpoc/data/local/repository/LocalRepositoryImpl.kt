package com.metromart.locationtrackignpoc.data.local.repository

import com.metromart.locationtrackignpoc.data.local.LocationDatabase
import com.metromart.locationtrackignpoc.data.local.entity.LocationEntity

class LocalRepositoryImpl(
    private val database: LocationDatabase
) : LocalRepository {
    
    override suspend fun insertLocationData(data: LocationEntity) {
        database.locationDao().insertLocationData(data)
    }

    override suspend fun getLocationData(): List<LocationEntity> {
        return database.locationDao().getLocationData()
    }
}
