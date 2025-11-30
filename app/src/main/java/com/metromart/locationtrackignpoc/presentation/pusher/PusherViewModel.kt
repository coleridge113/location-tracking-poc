package com.metromart.locationtrackignpoc.presentation.pusher

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.metromart.locationtrackignpoc.model.LocationData
import com.metromart.locationtrackignpoc.utils.pusher.Pusher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class PusherViewModel : ViewModel() {

    // Stream of locations from Pusher
    private val _locations =
        MutableSharedFlow<LocationData>(
            replay = 0,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    val locations: SharedFlow<LocationData> = _locations

    init {
        // Subscribe once per ViewModel (per route back stack entry)
        Pusher.subscribe { event ->
            try {
                val jsonString = event?.data.toString()
                val loc = Gson().fromJson(jsonString, LocationData::class.java)
                Log.d(
                    "PusherVM",
                    "parsed: seq=${loc.seq} lat=${loc.latitude}, lng=${loc.longitude}, ts=${loc.timestamp}"
                )
                _locations.tryEmit(loc)
            } catch (t: Throwable) {
                Log.e("PusherVM", "Error parsing Pusher event", t)
            }
        }
    }
}
