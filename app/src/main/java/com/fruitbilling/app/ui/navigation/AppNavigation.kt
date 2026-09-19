package com.fruitbilling.app.ui.navigation

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fruitbilling.app.FruitBillingApp
import com.fruitbilling.app.ui.billing.BillingScreen
import com.fruitbilling.app.ui.billing.BillingViewModel
import com.fruitbilling.app.ui.billing.BillingViewModelFactory
import com.fruitbilling.app.ui.billing.CalcScreen
import com.fruitbilling.app.ui.history.HistoryScreen
import com.fruitbilling.app.ui.history.HistoryViewModel
import com.fruitbilling.app.ui.history.HistoryViewModelFactory
import com.fruitbilling.app.ui.menu.MenuScreen
import com.fruitbilling.app.ui.menu.MenuViewModel
import com.fruitbilling.app.ui.menu.MenuViewModelFactory

@Composable
fun AppNavigation(
    app: FruitBillingApp,
    navController: NavHostController = rememberNavController()
) {
    val items = listOf(
        Screen.Billing,
        Screen.Calc,
        Screen.History,
        Screen.Menu
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Ensure status bar icons have high contrast on all screens:
    // History & Menu have dark green primary top bars -> crisp white icons.
    // Calc & Billing have surface headers -> dark icons in light mode, white in dark mode.
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            val isDarkTopBar = currentRoute == Screen.History.route || currentRoute == Screen.Menu.route
            insetsController.isAppearanceLightStatusBars = if (isDarkTopBar) false else !darkTheme
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                items.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.outline,
                            unselectedTextColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // Share a single BillingViewModel instance between Billing and Calc tabs
        // so both screens see the same bill state without duplication.
        val billingViewModel: BillingViewModel = viewModel(
            factory = BillingViewModelFactory(
                productRepository = app.productRepository,
                billRepository = app.billRepository
            )
        )

        NavHost(
            navController = navController,
            startDestination = Screen.Calc.route,    // Always open Calc first
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Billing tab: search / product picker (Beginner mode)
            composable(Screen.Billing.route) {
                BillingScreen(viewModel = billingViewModel)
            }

            // Calc tab: the smart calculator / bill pad (default landing screen)
            composable(Screen.Calc.route) {
                CalcScreen(viewModel = billingViewModel)
            }

            composable(Screen.History.route) {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModelFactory(
                        billRepository = app.billRepository
                    )
                )
                HistoryScreen(viewModel = historyViewModel)
            }

            composable(Screen.Menu.route) {
                val menuViewModel: MenuViewModel = viewModel(
                    factory = MenuViewModelFactory(
                        productRepository = app.productRepository,
                        database = app.database
                    )
                )
                MenuScreen(viewModel = menuViewModel)
            }
        }
    }
}
