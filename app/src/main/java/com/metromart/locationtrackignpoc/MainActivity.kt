package com.metromart.locationtrackignpoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.metromart.locationtrackignpoc.presentation.main.DrawerNavigation
import com.metromart.locationtrackignpoc.ui.theme.LocationTrackignPOCTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { 
            LocationTrackignPOCTheme {
                DrawerNavigation()
            } 
        }
    }
}

