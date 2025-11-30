package com.metromart.locationtrackignpoc.presentation.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.metromart.locationtrackignpoc.presentation.nav.NavGraphSetup
import com.metromart.locationtrackignpoc.presentation.nav.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerNavigation() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val drawerItems = listOf(
        DrawerItems.ABLY,
        DrawerItems.PUSHER
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val title = when (currentRoute) {
        Routes.AblyScreen::class.qualifiedName -> DrawerItems.ABLY.value
        Routes.PusherScreen::class.qualifiedName -> DrawerItems.PUSHER.value
        else -> "Ably"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet {
                LazyColumn {
                    items(drawerItems) { item ->
                        Text(
                            text = item.value,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 24.sp
                            ),
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        when (item) {
                                            DrawerItems.ABLY -> {
                                                navController.navigate(Routes.AblyScreen) {
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                            DrawerItems.PUSHER -> {
                                                navController.navigate(Routes.PusherScreen) {
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                        drawerState.close()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavGraphSetup(navController = navController)
        }
    }
}

enum class DrawerItems(val value: String) {
    ABLY("Ably"),
    PUSHER("Pusher")
}
