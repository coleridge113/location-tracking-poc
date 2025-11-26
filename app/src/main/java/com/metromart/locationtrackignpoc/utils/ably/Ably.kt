package com.metromart.locationtrackignpoc.utils.ably

import android.util.Log
import com.metromart.locationtrackignpoc.BuildConfig
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel

const val TAG = "Ably"

object Ably {
    val realtime: AblyRealtime by lazy {
        val client = AblyRealtime(BuildConfig.ABLY_API_KEY)

        // Log connection state
        client.connection.on { stateChange ->
            Log.d(
                TAG,
                "Android Ably connection: ${stateChange.previous} -> ${stateChange.current}" +
                (stateChange.reason?.let { " reason=${it.message}" } ?: "")
            )
        }

        client
    }

    fun subscribeToChannel(channelName: String, listener: Channel.MessageListener) {
        val channel = realtime.channels.get(channelName)

        // Log channel state
        channel.on { stateChange ->
            Log.d(
                TAG,
                "Channel[$channelName] state: ${stateChange.previous} -> ${stateChange.current}" +
                (stateChange.reason?.let { " reason=${it.message}" } ?: "")
            )
        }

        channel.subscribe(listener)

    }
}

