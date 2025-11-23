package com.metromart.locationtrackignpoc.presentation.nav

import kotlinx.serialization.Serializable

@Serializable
sealed class Routes {
    @Serializable
    data object MainRoute : Routes()
}

