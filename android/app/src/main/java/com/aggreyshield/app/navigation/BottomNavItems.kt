package com.aggreyshield.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

// Hidden until the backend has these endpoints: Dashboard, Incidents, Alerts (see docs/KNOWN_ISSUES.md).
val BOTTOM_NAV_ITEMS = listOf(
    BottomNavItem(Routes.SCANNER_HUB, "Scanner", Icons.Filled.Shield),
    BottomNavItem(Routes.PROFILE, "Profile", Icons.Filled.Person),
)
