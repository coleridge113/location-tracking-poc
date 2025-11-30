package com.metromart.locationtrackignpoc.presentation.pusher

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.extension.style.layers.generated.locationIndicatorLayer
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.metromart.locationtrackignpoc.BuildConfig
import com.metromart.locationtrackignpoc.model.LocationData
import com.metromart.locationtrackignpoc.utils.ably.Ably
import com.metromart.locationtrackignpoc.utils.pusher.Pusher
import io.ably.lib.realtime.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import com.mapbox.common.location.Location as MapboxLocation

@Composable
fun PusherScreen(navController: NavHostController) {
    val viewModel: PusherViewModel = viewModel()
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
        NavigationReceiverMapScreen(viewModel)
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
fun NavigationReceiverMapScreen(viewModel: PusherViewModel) {
    MapboxOptions.accessToken = BuildConfig.MAPBOX_DOWNLOADS_TOKEN

    val context = LocalContext.current
    val density = LocalDensity.current
    val realtimeThresholdMs = 2_000L

    val mapViewState = remember { mutableStateOf<MapView?>(null) }
    val navigationLocationProvider = remember { NavigationLocationProvider() }
    var viewportDataSource by remember { mutableStateOf<MapboxNavigationViewportDataSource?>(null) }
    var navigationCamera by remember { mutableStateOf<NavigationCamera?>(null) }

    var lastLoc by remember { mutableStateOf<MapboxLocation?>(null) }
    var latestServerTs by remember { mutableStateOf<Long?>(null) }

    // 1) Collect from ViewModel's flow and move the provider
    LaunchedEffect(Unit) {
        Log.d("PusherProcessor", "Processor started (VM-based)")
        viewModel.locations.collect { loc ->
            Log.d(
                "PusherProcessor",
                "processing seq=${loc.seq} lat=${loc.latitude}, lng=${loc.longitude}"
            )

            latestServerTs = loc.timestamp
            val newLoc = MapboxLocation.Builder()
                .latitude(loc.latitude)
                .longitude(loc.longitude)
                .timestamp(loc.timestamp)
                .build()

            val from = lastLoc
            val behindMs = (latestServerTs ?: loc.timestamp) - loc.timestamp
            val isBacklog = behindMs > realtimeThresholdMs
            val hugeBacklog = behindMs > 10_000L
            val hasBigGap = lastLoc != null &&
                (loc.timestamp - (lastLoc?.timestamp ?: loc.timestamp)) > 3_000L

            val (steps, stepDelay) = when {
                hugeBacklog -> 1 to 0L
                isBacklog || hasBigGap -> 4 to 10L
                else -> 15 to 60L
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

    // 2) MapView setup and binding
    val initialPoint = Point.fromLngLat(121.01877, 14.540679)

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
        update = { mapView ->
            mapView.location.apply {
                setLocationProvider(navigationLocationProvider)
                locationPuck = createDefault2DPuck(withBearing = false)
                enabled = true
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
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
        delay(stepDelayMs)
    }
}
