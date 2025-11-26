package com.metromart.locationtrackignpoc.model

import com.google.gson.annotations.SerializedName

data class LocationData(
    val type: String,
    val seq: Int,
    @SerializedName("lat")
    val latitude: Double,
    @SerializedName("lng")
    val longitude: Double,
    @SerializedName("ts")
    val timestamp: Long
)
