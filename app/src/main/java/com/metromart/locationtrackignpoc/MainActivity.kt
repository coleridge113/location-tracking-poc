package com.metromart.locationtrackignpoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.metromart.locationtrackignpoc.presentation.mapboxdemo.MapboxComposeApp
import com.metromart.locationtrackignpoc.presentation.main.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContent { MainScreen() }
        setContent { MapboxComposeApp() }
    }
}

