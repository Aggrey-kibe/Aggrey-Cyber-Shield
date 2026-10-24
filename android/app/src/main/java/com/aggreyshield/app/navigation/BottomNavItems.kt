package com.aggreyshield.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

val BOTTOM_NAV_ITEMS = listOf(
    BottomNavItem(Routes.DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
    BottomNavItem(Routes.SCANNER_HUB, "Scanner", Icons.Filled.Shield),
    BottomNavItem(Routes.INCIDENT_LIST, "Incidents", Icons.Filled.Warning),
    BottomNavItem(Routes.ALERTS, "Alerts", Icons.Filled.NotificationImportant),
    BottomNavItem(Routes.PROFILE, "Profile", Icons.Filled.Person),
)
