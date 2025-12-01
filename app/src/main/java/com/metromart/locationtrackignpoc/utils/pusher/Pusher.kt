package com.metromart.locationtrackignpoc.utils.pusher

import android.util.Log
import com.metromart.locationtrackignpoc.BuildConfig
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.Channel
import com.pusher.client.channel.ChannelEventListener
import com.pusher.client.channel.PrivateChannel
import com.pusher.client.channel.PrivateChannelEventListener
import com.pusher.client.channel.PusherEvent
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object Pusher {
    private val TAG = Pusher::class.java.simpleName

    private const val AUTH_URL = "http://192.168.100.70:3000/pusher/auth"
    private val httpClient = OkHttpClient()
    private val options: PusherOptions = PusherOptions().apply {
        setCluster(BuildConfig.PUSHER_CLUSTER)

        setChannelAuthorizer { channelName, socketId ->
            try {
                val bodyJson = JSONObject().apply {
                    put("channel_name", channelName)
                    put("socket_id", socketId)
                }

                val body = bodyJson
                    .toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url(AUTH_URL)
                    .post(body)
                    .build()

                httpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.e(TAG, "Pusher auth failed: HTTP ${resp.code}")
                        null
                    } else {
                        val authResponse = resp.body?.string()
                        Log.d(TAG, "Pusher auth response: $authResponse")
                        authResponse
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Pusher auth error: ${e.message}", e)
                null
            }
        }
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
        pusher.connect(listener, ConnectionState.ALL)
    }

    fun subscribe(
        onEvent: (PusherEvent?) -> Unit
    ): PrivateChannel {
        val channelName = "private-psher-channel"
        val eventName = "client-psher-route"

        val channel = pusher.subscribePrivate(
            channelName,
            object : PrivateChannelEventListener {
                override fun onSubscriptionSucceeded(channelName: String?) {
                    Log.d(TAG, "Subscription succeeded: $channelName")
                }

                override fun onEvent(event: PusherEvent?) {
                    Log.d(TAG, "Event: ${event?.eventName} data=${event?.data}")
                    onEvent(event)
                }

                override fun onAuthenticationFailure(
                    message: String?,
                    e: java.lang.Exception?
                ) {
                    Log.d(TAG, "Error: $message, ${e?.message}")
                }
            },
            eventName
        )
        return channel
    }

    fun trigger() {
    }
}
