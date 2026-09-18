package com.fruitbilling.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Billing : Screen("billing", "Billing", Icons.Default.PointOfSale)
    data object Calc   : Screen("calc",    "Calc",    Icons.Default.Calculate)
    data object History: Screen("history", "History", Icons.Default.History)
    data object Menu   : Screen("menu",    "Menu",    Icons.Default.Restaurant)
}
