package com.metromart.locationtrackignpoc.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.metromart.locationtrackignpoc.data.local.dao.LocationDao

@Database(
    entities = [LocationDao::class],
    version = 1,
    exportSchema = false
)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationDao() : LocationDao
}

