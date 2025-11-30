package com.metromart.locationtrackignpoc.utils.pusher

import android.util.Log
import com.metromart.locationtrackignpoc.BuildConfig
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.ChannelEventListener
import com.pusher.client.channel.PusherEvent
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange

object Pusher {
    private val TAG = Pusher::class.java.simpleName

    private val options = PusherOptions().apply {
        setCluster(BuildConfig.PUSHER_CLUSTER)
        setUseTLS(true)
    }

    val pusher = Pusher(BuildConfig.PUSHER_KEY, options)

    private val listener = object : ConnectionEventListener {
        override fun onConnectionStateChange(change: ConnectionStateChange?) {
            Log.d(
                TAG,
                "Connection state: ${change?.previousState} -> ${change?.currentState}"
            )
        }

        override fun onError(message: String?, code: String?, e: Exception?) {
            Log.e(TAG, "Connection error: message=$message code=$code", e)
        }
    }

    init {
        Log.d(TAG, "API_KEY: ${BuildConfig.PUSHER_API_KEY}")
        Log.d(TAG, "Cluster: ${BuildConfig.PUSHER_CLUSTER}")
        pusher.connect(listener, ConnectionState.ALL)
    }

    fun subscribe() {
        val channelName = "psher-channel"
        val eventName = "psher-route"

        val existingChannel = pusher.getChannel(channelName)
        if (existingChannel != null) {
            return
        }

        pusher.subscribe(
            channelName,
            object : ChannelEventListener {
                override fun onSubscriptionSucceeded(channelName: String?) {
                    Log.d(TAG, "Subscription succeeded: $channelName")
                }

                override fun onEvent(event: PusherEvent?) {
                    Log.d(TAG, "Event: ${event?.eventName} data=${event?.data}")
                }
            },
            eventName
        )
    }
}
