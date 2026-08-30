package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Pos : Screen("pos")
    object Inventory : Screen("inventory")
    object Reports : Screen("reports")
    object Settings : Screen("settings")
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

val bottomNavItems = listOf(
    BottomNavItem(
        title = "Kasir POS",
        route = Screen.Pos.route,
        selectedIcon = Icons.Filled.PointOfSale,
        unselectedIcon = Icons.Outlined.PointOfSale
    ),
    BottomNavItem(
        title = "Stok Produk",
        route = Screen.Inventory.route,
        selectedIcon = Icons.Filled.Inventory2,
        unselectedIcon = Icons.Outlined.Inventory2
    ),
    BottomNavItem(
        title = "Laporan Harian",
        route = Screen.Reports.route,
        selectedIcon = Icons.Filled.Assessment,
        unselectedIcon = Icons.Outlined.Assessment
    ),
    BottomNavItem(
        title = "Pengaturan",
        route = Screen.Settings.route,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)
