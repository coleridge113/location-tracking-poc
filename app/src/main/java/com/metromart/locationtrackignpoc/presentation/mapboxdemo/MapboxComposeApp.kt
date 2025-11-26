package com.metromart.locationtrackignpoc.presentation.mapboxdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.MapboxOptions
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.metromart.locationtrackignpoc.BuildConfig

@Composable
fun MapboxComposeApp() {
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
        NavigationMapScreen()
    } else {
        Box(Modifier.fillMaxSize()) {
            Button(onClick = {
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
            }) { Text("Grant location permission") }
        }
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@Composable
fun NavigationMapScreen() {
    val context = LocalContext.current
    val density = LocalDensity.current

    MapboxOptions.accessToken = BuildConfig.MAPBOX_DOWNLOADS_TOKEN

    val routeCoordinates = remember { mutableStateListOf<Point>() }
    val traveledCoordinates = remember { mutableStateListOf<Point>() }    

    // Map + Navigation state holders
    val mapViewState = remember { mutableStateOf<MapView?>(null) }
    val navigationLocationProvider = remember { NavigationLocationProvider() }
    val replayRouteMapper = remember { ReplayRouteMapper() }

    // Route line + camera state
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

    // Create MapboxNavigation once for this screen
    val mapboxNavigation = remember {
        MapboxNavigationProvider.create(
            NavigationOptions.Builder(context).build()
        )
    }

    // Observers
    val routesObserver = remember {
        com.mapbox.navigation.core.directions.session.RoutesObserver { routeUpdate ->
            val mv = mapViewState.value ?: return@RoutesObserver
            if (routeUpdate.navigationRoutes.isNotEmpty()) {
                // draw the route
                routeLineApi.setNavigationRoutes(routeUpdate.navigationRoutes) { drawData ->
                    mv.mapboxMap.style?.let { style ->
                        routeLineView.renderRouteDrawData(style, drawData)
                    }
                }

                // update viewport and go to overview
                viewportDataSource?.onRouteChanged(routeUpdate.navigationRoutes.first())
                viewportDataSource?.evaluate()
                navigationCamera?.requestNavigationCameraToOverview()
            }
        }
    }

    val locationObserver = remember {
        object : LocationObserver {
            override fun onNewRawLocation(rawLocation: Location) {}

            override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
                val enhanced = locationMatcherResult.enhancedLocation
                navigationLocationProvider.changePosition(
                    location = enhanced,
                    keyPoints = locationMatcherResult.keyPoints
                )
                viewportDataSource?.onLocationChanged(enhanced)
                viewportDataSource?.evaluate()
                navigationCamera?.requestNavigationCameraToFollowing()
            }
        }
    }

    // Build the MapView inside Compose
    val avalu = Point.fromLngLat(121.0306, 14.5659)
    val rockwell = Point.fromLngLat(121.0367, 14.5636)
    val snrMakati = Point.fromLngLat(121.018857, 14.540726)
    val commonGround = Point.fromLngLat(121.036668, 14.563586)
    val ronac = Point.fromLngLat(121.040214, 14.607190)
    val cpark = Point.fromLngLat(121.018910, 14.578097)
    val golf = Point.fromLngLat(121.040689, 14.531111)
    val clubhouse = Point.fromLngLat(121.04157921602086, 14.537736243699587) 

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

    // Wire up Navigation lifecycle with Compose
    DisposableEffect(Unit) {
        // Register observers and kick off a replayed trip session
        val mv = mapViewState.value
        if (mv != null) {
            // ensure the nice default puck
            mv.location.apply {
                setLocationProvider(navigationLocationProvider)
                locationPuck = createDefault2DPuck()
                enabled = true
            }
        }

        val replayProgressObserver = ReplayProgressObserver(mapboxNavigation.mapboxReplayer)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.startReplayTripSession()

        // Request a simple 2-point route and push replay events
        val origin = snrMakati
        val destination = clubhouse

        @SuppressLint("MissingPermission")
        fun requestRoute() {
            mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .coordinatesList(listOf(origin, destination))
                    .layersList(listOf(mapboxNavigation.getZLevel(), null))
                    .build(),
                object : NavigationRouterCallback {
                    override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {}
                    override fun onFailure(
                        reasons: List<RouterFailure>,
                        routeOptions: RouteOptions
                    ) {}

                    override fun onRoutesReady(
                        routes: List<NavigationRoute>,
                        routerOrigin: String
                    ) {
                        mapboxNavigation.setNavigationRoutes(routes)

                        val first = routes.first().directionsRoute
                        val geometry = first.geometry()
                        if (geometry != null) {
                            val decoded: List<Point> = PolylineUtils.decode(geometry, 6)
                            routeCoordinates.clear()
                            routeCoordinates.addAll(decoded)
                            Log.d("RoutePoints", "First 6: " +
                                    routeCoordinates.take(6).joinToString { "${it.longitude()},${it.latitude()}" }
                            )
                            Log.d("RoutePoints", "Total route ponts: ${routeCoordinates.size}")
                           val content = routeCoordinates.joinToString("\n") {
                               "${it.latitude()},${it.longitude()}"
                           }

                           // Copy to clipboard instead of writing file
//                           try {
//                               val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//                               val clip = ClipData.newPlainText("route_points", content)
//                               clipboard.setPrimaryClip(clip)
//                               Log.d("RoutePointsClipboard", "Copied ${routeCoordinates.size} points to clipboard.")
//                               Toast.makeText(context, "Route points copied to clipboard", Toast.LENGTH_SHORT).show()
//                           } catch (e: Exception) {
//                               Log.e("RoutePointsClipboard", "Failed to copy", e)
//                           }
                        }

                        // Simulate user movement along the route
                        val replayData = replayRouteMapper
                            .mapDirectionsRouteGeometry(routes.first().directionsRoute)
                        mapboxNavigation.mapboxReplayer.pushEvents(replayData)
                        mapboxNavigation.mapboxReplayer.seekTo(replayData.first())
                        mapboxNavigation.mapboxReplayer.play()
                    }
                }
            )
        }

        requestRoute()

        onDispose {
            // Unregister and clean up
            mapboxNavigation.unregisterRoutesObserver(routesObserver)
            mapboxNavigation.unregisterLocationObserver(locationObserver)
            mapboxNavigation.stopTripSession()
            MapboxNavigationProvider.destroy() // releases the singleton instance
            mapViewState.value = null
        }
    }
}
