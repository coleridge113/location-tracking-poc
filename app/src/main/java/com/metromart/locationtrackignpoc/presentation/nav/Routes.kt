package com.metromart.locationtrackignpoc.presentation.nav

import kotlinx.serialization.Serializable

@Serializable
sealed class Routes {
    @Serializable
    data object MainRoute : Routes()

    @Serializable
    data object PermissionsRoute : Routes()

    @Serializable
    data object RadarRoute : Routes()
}

