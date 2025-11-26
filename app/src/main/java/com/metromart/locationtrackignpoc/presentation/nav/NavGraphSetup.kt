package com.metromart.locationtrackignpoc.presentation.nav

import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.compose.composable
import com.metromart.locationtrackignpoc.presentation.main.MainScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraphSetup(
    navController: NavHostController
) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Routes.MainRoute,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            composable<Routes.MainRoute> {
                MainScreen()
            }
        } 
    }
}
