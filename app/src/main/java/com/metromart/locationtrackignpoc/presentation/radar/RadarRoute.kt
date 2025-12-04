package com.metromart.locationtrackignpoc.presentation.radar

import android.location.Location
import android.util.Log
import android.view.Gravity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.metromart.locationtrackignpoc.BuildConfig
import com.metromart.locationtrackignpoc.R
import io.radar.sdk.Radar
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarRoute() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Radar",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
    ) { innerPadding ->
        MainContent(Modifier.padding(innerPadding))
    }
}

@Composable
fun MainContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    MapLibre.getInstance(context)
    val mapView = remember { MapView(context) }
    var point by remember { mutableStateOf(GeoPoint(14.540678, 121.01877)) }

    var mapLibreMap by remember {
        mutableStateOf<MapLibreMap?>(null)
    }

    val style = "radar-default-v1"
    val publishableKey = BuildConfig.RADAR_TEST_PUBLISHABLE
    val styleURL = "https://api.radar.io/maps/styles/$style?publishableKey=$publishableKey"

    val origin = Location("mock")
    origin.latitude = 14.540678
    origin.longitude = 121.01877

    val destination = Location("mock")
    destination.latitude = 14.56582
    destination.longitude = 121.030562

    var symbolManager by remember { mutableStateOf<SymbolManager?>(null) }
    var symbol by remember { mutableStateOf<Symbol?>(null) }

    LaunchedEffect(Unit) {
        Radar.mockTracking(
            origin,
            destination,
            Radar.RadarRouteMode.CAR,
            1,
            1
        ) { status, location, events, user ->
            Log.d("LaunchedEffect", "Output: $status, $location, $events, $user")
            location?.let {
                point = GeoPoint(
                    lat = it.latitude,
                    lng = it.longitude
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            mapLibreMap = map
            map.setStyle(styleURL) { style ->
                map.uiSettings.apply {
                    isLogoEnabled = false
                    attributionGravity = Gravity.END + Gravity.BOTTOM
                    setAttributionMargins(0, 0, 24, 24)
                }

                map.cameraPosition = CameraPosition.Builder()
                    .target(point.toPoint())
                    .zoom(16.0)
                    .build()

                val sm = SymbolManager(mapView, map, style)
                symbolManager = sm

                val s = sm.create(
                    SymbolOptions()
                    .withLatLng(point.toPoint())
                    .withIconImage("wheelchair")
                )
                symbol = s
            }
        }
    }

    AndroidView(
        factory = { mapView },
        update = { CameraUpdateFactory.newLatLng(point.toPoint()) },
        modifier = Modifier.fillMaxSize()
    )

    LaunchedEffect(point) {
        Log.d("LaunchedEffect", "Throwing new point via Launched Effect: $point")
        symbol?.let { s ->
            symbolManager?.let { sm ->
                s.latLng = point.toPoint()
                sm.update(s)
            }
        }
        mapLibreMap?.animateCamera(
            CameraUpdateFactory.newLatLng(point.toPoint()),
            2500
        )
    }
}

private fun Location.toPoint(): LatLng {
    return LatLng().apply {
        latitude = latitude
        longitude = longitude
    }
}

private fun GeoPoint.toPoint(): LatLng {
    return LatLng().apply {
        latitude = lat
        longitude = lng
    }
}
data class GeoPoint(val lat: Double, val lng: Double)
