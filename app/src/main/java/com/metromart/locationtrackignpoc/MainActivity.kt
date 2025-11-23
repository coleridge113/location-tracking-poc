package com.metromart.locationtrackignpoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.metromart.locationtrackignpoc.presentation.nav.NavGraphSetup
import com.metromart.locationtrackignpoc.ui.theme.LocationTrackignPOCTheme
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.android.core.permissions.PermissionsListener

class MainActivity : ComponentActivity() {
    private lateinit var permissionsManager: PermissionsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocationTrackignPOCTheme {
                var permissionGranted by remember { mutableStateOf(PermissionsManager.areLocationPermissionsGranted(this)) }
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    if (!permissionGranted) {
                        permissionsManager = PermissionsManager(object : PermissionsListener {
                            override fun onExplanationNeeded(permissionsToExplain: List<String>) {}
                            override fun onPermissionResult(granted: Boolean) {
                                permissionGranted = granted
                            }
                        })
                        permissionsManager.requestLocationPermissions(this@MainActivity)
                    }
                }

                if (permissionGranted) {
                    NavGraphSetup(navController)
                } else {
                    Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                        Text("Requesting location permission...")
                    }
                }
            }
        }
    }
}
