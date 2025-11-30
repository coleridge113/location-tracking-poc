package com.metromart.locationtrackignpoc.presentation.ably

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
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
import com.metromart.locationtrackignpoc.model.LocationData
import com.metromart.locationtrackignpoc.presentation.pusher.NavigationReceiverMapScreen
import com.metromart.locationtrackignpoc.utils.ably.Ably
import com.metromart.locationtrackignpoc.utils.pusher.Pusher
import io.ably.lib.realtime.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import com.mapbox.common.location.Location as MapboxLocation

@Composable
fun AblyScreen(navController: NavHostController) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermission = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!hasPermission) {
            Toast.makeText(
                context,
                "Location permission denied. Enable it in settings.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    if (hasPermission) {
        NavigationReceiverMapScreen()
    } else {
        Box(Modifier.fillMaxSize()) {
            Button(onClick = {
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
            }) { Text("Grant location permission") }
        }
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@SuppressLint("VisibleForTests")
@Composable
fun NavigationReceiverMapScreen() {
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

    LaunchedEffect(Unit) {
        val channelName = "ably-channel"
        val listener = Channel.MessageListener { message ->
            try {
                val jsonString = message.data.toString()
                val loc = Gson().fromJson(jsonString, LocationData::class.java)

                Log.d(
                    "AblyChannel",
                    "parsed: seq=${loc.seq} lat=${loc.latitude}, lng=${loc.longitude}, ts=${loc.timestamp}"
                )

                // just enqueue, do not move the puck directly here
                bufferedPoints += loc
                latestServerTs = loc.timestamp

                // update lastServerTs to detect gaps
                lastServerTs = loc.timestamp
            } catch (t: Throwable) {
                Log.e("AblyChannel", "Error in message listener", t)
            }
        }

        Ably.subscribeToChannel(channelName, listener)
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
            // We never started a trip session, so no need to stop it.
            // Just destroy the shared MapboxNavigation instance.
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
