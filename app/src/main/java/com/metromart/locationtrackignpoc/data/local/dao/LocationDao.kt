package com.metromart.locationtrackignpoc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.metromart.locationtrackignpoc.data.local.entity.LocationEntity

@Dao
interface LocationDao {
    @Insert
    suspend fun insertLocationData(data: LocationEntity)

    @Query("SELECT * FROM location")
    suspend fun getLocationData(): List<LocationEntity>

}
