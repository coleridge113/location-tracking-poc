package com.metromart.locationtrackignpoc.data.local.entity

import androidx.room.Entity

@Entity(tableName = "location")
data class LocationEntity(
    val type: String,
    val seq: Int,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
