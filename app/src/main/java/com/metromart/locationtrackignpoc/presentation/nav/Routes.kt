package com.metromart.locationtrackignpoc.presentation.nav

import kotlinx.serialization.Serializable

@Serializable
sealed class Routes {
    @Serializable
    data object AblyScreen : Routes()

    @Serializable
    data object PusherScreen : Routes()
}

