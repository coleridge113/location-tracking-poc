package com.metromart.locationtrackignpoc.presentation.radar

import android.content.Context
import android.location.Location
import io.radar.sdk.Radar
import io.radar.sdk.RadarReceiver
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarUser
import android.util.Log

class MyRadarReceiver : RadarReceiver() {
    override fun onClientLocationUpdated(
        context: Context,
        location: Location,
        stopped: Boolean,
        source: Radar.RadarLocationSource
    ) {
        Log.d("MyRadarReceiver", "onClientLocationUpdated: lat=${location.latitude}, lng=${location.longitude}, stopped=$stopped, source=$source")
    }

    override fun onError(context: Context, status: Radar.RadarStatus) {
        Log.d("MyRadarReceiver", "onError: status=$status")
    }

    override fun onEventsReceived(
        context: Context,
        events: Array<RadarEvent>,
        user: RadarUser?
    ) {
        Log.d("MyRadarReceiver", "onEventsReceived: eventsCount=${events.size}, userId=${user?.userId}")
        events.forEach { event ->
            Log.d("MyRadarReceiver", "Event: type=${event.type}, confidence=${event.confidence}, geofence=${event.geofence?.description}")
        }
    }

    override fun onLocationUpdated(
        context: Context,
        location: Location,
        user: RadarUser
    ) {
        Log.d("MyRadarReceiver", "onLocationUpdated: lat=${location.latitude}, lng=${location.longitude}, userId=${user.userId}")
    }

    override fun onLog(context: Context, message: String) {
        Log.d("MyRadarReceiver", "onLog: $message")
    }
}
