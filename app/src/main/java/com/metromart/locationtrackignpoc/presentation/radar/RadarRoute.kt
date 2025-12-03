package com.metromart.locationtrackignpoc.presentation.radar

import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.radar.sdk.Radar

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
    val origin = Location("mock")
    origin.latitude = 40.78382
    origin.longitude = -73.97536

    val destination = Location("mock")
    destination.latitude = 40.70390
    destination.longitude = -73.98670

    Radar.mockTracking(
        origin,
        destination,
        Radar.RadarRouteMode.CAR,
        10,
        3) { status, location, events, user ->
            Log.d("Radar", "Output: $status, $location, $events, $user")
        }
}
