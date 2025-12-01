package com.metromart.locationtrackignpoc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val seq: Int,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
