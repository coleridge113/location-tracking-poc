package com.metromart.locationtrackignpoc.presentation.main

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.metromart.locationtrackignpoc.BuildConfig
import com.metromart.locationtrackignpoc.utils.ably.Ably
import io.ably.lib.realtime.Channel

@Composable
fun MainScreen() {
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

@Composable
fun NavigationReceiverMapScreen() {
    MapboxOptions.accessToken = BuildConfig.MAPBOX_DOWNLOADS_TOKEN
    
    LaunchedEffect(Unit) {
        val listener = Channel.MessageListener { message ->
            Log.d("AblyChannel", "${message.data}")
        }
        Ably.subscribeToChannel("ably-channel", listener)
        Ably.publishToChannel("ably-channel")
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val mapViewState = remember { mutableStateOf<MapView?>(null) }
    val navigationLocationProvider = remember { NavigationLocationProvider() }
    val replayRouteMapper = remember { ReplayRouteMapper() }
    var viewportDataSource by remember { mutableStateOf<MapboxNavigationViewportDataSource?>(null) }
    var navigationCamera by remember { mutableStateOf<NavigationCamera?>(null) }
    val routeLineApi by remember {
        mutableStateOf(MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build()))
    }
    val routeLineView by remember {
        mutableStateOf(
            MapboxRouteLineView(
                MapboxRouteLineViewOptions.Builder(context).build()
            )
        )
    }

    val avalu = Point.fromLngLat(121.0306, 14.5659)
    val rockwell = Point.fromLngLat(121.0367, 14.5636)
    val snrMakati = Point.fromLngLat(121.018857, 14.540726)
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(snrMakati)
                        .zoom(14.0)
                        .pitch(5.0)
                        .build()
                )
                // Enable location puck using NavigationLocationProvider
                location.apply {
                    setLocationProvider(navigationLocationProvider)
                    locationPuck = LocationPuck2D() // replaced later with default 2D puck as well
                    enabled = true
                }
                mapViewState.value = this

                // Viewport, padding, and camera
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
}
