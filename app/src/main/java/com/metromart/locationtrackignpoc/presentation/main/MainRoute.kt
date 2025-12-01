package com.metromart.locationtrackignpoc.presentation.main

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.Gson
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.metromart.locationtrackignpoc.BuildConfig
import com.metromart.locationtrackignpoc.data.local.entity.LocationEntity
import com.metromart.locationtrackignpoc.data.local.repository.LocalRepository
import com.metromart.locationtrackignpoc.model.LocationData
import com.metromart.locationtrackignpoc.utils.ably.Ably
import com.metromart.locationtrackignpoc.utils.pusher.Pusher
import io.ably.lib.realtime.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.mapbox.common.location.Location as MapboxLocation


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainRoute(
    viewModel: MainViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var messagingProvider by remember { mutableStateOf(uiState.messagingProvider) }

    val onEvent = remember(viewModel) { viewModel::onEvent }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        messagingProvider.value,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        modifier = Modifier.clickable {
            onEvent(MainViewModelStateEvent.Event.ToggleProvider)
        }
    ) { innerPadding ->
        MainScreen(
            modifier = Modifier.padding(innerPadding),
            messagingProvider = messagingProvider
        )

    }
    
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@SuppressLint("VisibleForTests")
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    messagingProvider: String,
    repository: LocalRepository = koinInject()
) {
    MapboxOptions.accessToken = BuildConfig.MAPBOX_DOWNLOADS_TOKEN

    val context = LocalContext.current
    val density = LocalDensity.current
    val realtimeThresholdMs = 2_000L
    val mapViewState = remember { mutableStateOf<MapView?>(null) }
    val navigationLocationProvider = remember { NavigationLocationProvider() }
    var viewportDataSource by remember { mutableStateOf<MapboxNavigationViewportDataSource?>(null) }
    var navigationCamera by remember { mutableStateOf<NavigationCamera?>(null) }

    // we only need MapboxNavigation for NavigationCamera & viewport helpers (no replay)
    val mapboxNavigation = remember {
        MapboxNavigationProvider.create(
            NavigationOptions.Builder(context).build()
        )
    }

    var lastLoc by remember { mutableStateOf<MapboxLocation?>(null) }
    val uiScope = remember { CoroutineScope(Dispatchers.Main) }
    val bufferedPoints = remember { mutableStateListOf<LocationData>() }
    var lastServerTs by remember { mutableStateOf<Long?>(null) }
    var isReplaying by remember { mutableStateOf(false) }
    var latestServerTs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(messagingProvider) {
        val ablyChannelName = "ably-channel"

        var ablyChannel: io.ably.lib.realtime.Channel? = null
        var ablyListener: Channel.MessageListener? = null
        var pusherChannel: com.pusher.client.channel.Channel? = null

        try {
            when (messagingProvider) {
                LocationProviderType.ABLY -> {
                    val listener = Channel.MessageListener { message ->
                        try {
                            val jsonString = message.data.toString()
                            val loc = Gson().fromJson(jsonString, LocationData::class.java)

                            Log.d(
                                "AblyChannel",
                                "parsed: seq=${loc.seq} lat=${loc.latitude}, lng=${loc.longitude}, ts=${loc.timestamp}"
                            )

                            bufferedPoints += loc
                            latestServerTs = loc.timestamp
                            lastServerTs = loc.timestamp
                        } catch (t: Throwable) {
                            Log.e("AblyChannel", "Error in message listener", t)
                        }
                    }
                    ablyChannel = Ably.realtime.channels.get(ablyChannelName)
                    ablyListener = listener
                    ablyChannel?.subscribe(listener)
                }

                LocationProviderType.PUSHER -> {
                    pusherChannel = Pusher.subscribe { event ->
                        try {
                            val jsonString = event?.data.toString()
                            val loc = Gson().fromJson(jsonString, LocationData::class.java)

                            Log.d(
                                "PusherChannel",
                                "parsed: seq=${loc.seq} lat=${loc.latitude}, lng=${loc.longitude}, ts=${loc.timestamp}"
                            )

                            bufferedPoints += loc
                            latestServerTs = loc.timestamp
                            lastServerTs = loc.timestamp

                            uiScope.launch {
                                repository.insertLocationData(
                                    LocationEntity(
                                        type = loc.type,
                                        seq = loc.seq,
                                        latitude = loc.latitude,
                                        longitude = loc.longitude,
                                        timestamp = loc.timestamp
                                    )
                                )
                            }
                        } catch (t: Throwable) {
                            Log.e("PusherChannel", "Error in event listener", t)
                        }
                    }
                }
            }

            // Keep this effect alive until providerType changes
            // (LaunchedEffect will cancel when key changes)
            while (true) {
                delay(60_000L)
            }
        } finally {
            try {
                ablyChannel?.let { ch ->
                    ablyListener?.let { lst -> ch.unsubscribe(lst) }
                }
            } catch (t: Throwable) {
                Log.e("AblyChannel", "Error unsubscribing", t)
            }

            try {
                pusherChannel?.let { ch ->
                    Pusher.pusher.unsubscribe(ch.name)
                }
            } catch (t: Throwable) {
                Log.e("PusherChannel", "Error unsubscribing", t)
            }
        }
    }

    // 2) Processor loop: consume bufferedPoints in order and animate
    LaunchedEffect(Unit) {
        while (true) {
            if (bufferedPoints.isEmpty()) {
                delay(20)
                continue
            }

            val loc = bufferedPoints.removeAt(0)

            val newLoc = MapboxLocation.Builder()
            .latitude(loc.latitude)
            .longitude(loc.longitude)
            .timestamp(loc.timestamp)
            .build()

            val from = lastLoc

            // How far behind this point is compared to the latest we've seen
            val behindMs = (latestServerTs ?: loc.timestamp) - loc.timestamp
            val isBacklog = behindMs > realtimeThresholdMs

            // Also detect big jump between lastLoc and this loc (true offline gap)
            val hasBigGap = lastLoc != null &&
            (loc.timestamp - (lastLoc?.timestamp ?: loc.timestamp)) > 3_000L

            // If we have *a lot* of backlog (e.g. > 10s), snap instead of animating
            val hugeBacklog = behindMs > 10_000L

            val (steps, stepDelay) = when {
                hugeBacklog -> 1 to 0L        // snap through very old data
                isBacklog || hasBigGap -> 4 to 10L   // fast catch-up
                else -> 15 to 60L             // normal smooth
            }

            if (from == null || steps <= 1) {
                navigationLocationProvider.changePosition(newLoc, emptyList())
            } else {
                animateBetween(
                    from = from,
                    to = newLoc,
                    provider = navigationLocationProvider,
                    steps = steps,
                    stepDelayMs = stepDelay
                )
            }
            lastLoc = newLoc

            viewportDataSource?.onLocationChanged(newLoc)
            viewportDataSource?.evaluate()
            navigationCamera?.requestNavigationCameraToFollowing()
        }
    }

    val snrMakati = Point.fromLngLat(121.018857, 14.540726)
    val initialPoint = snrMakati
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(initialPoint)
                        .zoom(14.0)
                        .pitch(5.0)
                        .build()
                )

                val startLoc = MapboxLocation.Builder()
                .latitude(initialPoint.latitude())
                .longitude(initialPoint.longitude())
                .timestamp(System.currentTimeMillis())
                .build()
                navigationLocationProvider.changePosition(startLoc, emptyList())
                lastLoc = startLoc

                location.apply {
                    setLocationProvider(navigationLocationProvider)
                    locationPuck = createDefault2DPuck(withBearing = false)
                    enabled = true
                }
                mapViewState.value = this

                viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap).also { vds ->
                    val top = with(density) { 180.dp.toPx().toDouble() }
                    val left = with(density) { 40.dp.toPx().toDouble() }
                    val bottom = with(density) { 500.dp.toPx().toDouble() }
                    val right = with(density) { 40.dp.toPx().toDouble() }
                    vds.followingPadding = EdgeInsets(top, left, bottom, right)
                }
                navigationCamera = NavigationCamera(mapboxMap, camera, viewportDataSource!!)
            }
        },
        update = { /* no-op */ }
    )

    DisposableEffect(Unit) {
        // ensure puck stays configured with our provider
        mapViewState.value?.location?.apply {
            setLocationProvider(navigationLocationProvider)
            locationPuck = createDefault2DPuck(withBearing = false)
            enabled = true
        }

        onDispose {
            mapboxNavigation.stopTripSession()
            MapboxNavigationProvider.destroy()
            mapViewState.value = null
        }
    }
}

private suspend fun animateBetween(
    from: MapboxLocation,
    to: MapboxLocation,
    provider: NavigationLocationProvider,
    steps: Int = 15,
    stepDelayMs: Long = 60L,
) {
    for (i in 1..steps) {
        val t = i / steps.toDouble()
        val lat = from.latitude + (to.latitude - from.latitude) * t
        val lng = from.longitude + (to.longitude - from.longitude) * t
        val stepLoc = MapboxLocation.Builder()
            .latitude(lat)
            .longitude(lng)
            .build()
        provider.changePosition(stepLoc, emptyList())
        kotlinx.coroutines.delay(stepDelayMs)
    }
}

enum class LocationProviderType(val value: String) {
    ABLY("Ably"),
    PUSHER("Pusher")
}

