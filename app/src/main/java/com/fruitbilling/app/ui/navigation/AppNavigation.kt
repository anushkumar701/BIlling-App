package com.fruitbilling.app.ui.navigation

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.fruitbilling.app.data.backup.CloudBackupManager
import com.fruitbilling.app.data.backup.GoogleAuthManager
import com.fruitbilling.app.ui.billing.BillingScreen
import com.fruitbilling.app.ui.billing.BillingViewModel
import com.fruitbilling.app.ui.billing.BillingViewModelFactory
import com.fruitbilling.app.ui.billing.CalcScreen
import com.fruitbilling.app.ui.common.OnboardingDialog
import com.fruitbilling.app.ui.common.SimpleUpdateDialog
import com.fruitbilling.app.ui.history.HistoryScreen
import com.fruitbilling.app.ui.history.HistoryViewModel
import com.fruitbilling.app.ui.history.HistoryViewModelFactory
import com.fruitbilling.app.ui.menu.MenuScreen
import com.fruitbilling.app.ui.menu.MenuViewModel
import com.fruitbilling.app.ui.menu.MenuViewModelFactory
import com.fruitbilling.app.util.AppReleaseInfo
import com.fruitbilling.app.util.OtaUpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isOnboardingOpen by remember { mutableStateOf(!GoogleAuthManager.isOnboardingCompleted(context)) }
    var launchUpdateInfo by remember { mutableStateOf<AppReleaseInfo?>(null) }
    val driveConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val email = GoogleAuthManager.getLastSignedInAccount(context)?.email ?: return@rememberLauncherForActivityResult
        coroutineScope.launch(Dispatchers.IO) {
            val result = CloudBackupManager.restoreFromCloud(context, app.database, email)
            withContext(Dispatchers.Main) {
                result.onSuccess { stats ->
                    if (stats.totalCount > 0) {
                        android.widget.Toast.makeText(
                            context,
                            "✅ Restored ${stats.productsRestored} fruits & ${stats.billsRestored} bills from Google Drive!",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    val onboardingSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val userResult = GoogleAuthManager.handleSignInResult(context, result.data)
        userResult.onSuccess { user ->
            GoogleAuthManager.setOnboardingCompleted(context)
            isOnboardingOpen = false
            val email = user.email ?: ""
            if (email.isNotBlank()) {
                coroutineScope.launch(Dispatchers.IO) {
                    val restoreResult = CloudBackupManager.restoreFromCloud(context, app.database, email)
                    withContext(Dispatchers.Main) {
                        restoreResult.onSuccess { stats ->
                            if (stats.totalCount > 0) {
                                android.widget.Toast.makeText(
                                    context,
                                    "✅ Welcome back, ${user.displayName ?: email}! Restored ${stats.productsRestored} fruits & ${stats.billsRestored} bills from cloud.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } else {
                                android.widget.Toast.makeText(context, "✅ Signed in as $email", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }.onFailure {
                            val consentIntent = com.fruitbilling.app.data.backup.GoogleDriveManager.pendingAuthIntent
                            if (consentIntent != null) {
                                driveConsentLauncher.launch(consentIntent)
                            } else {
                                android.widget.Toast.makeText(context, "✅ Signed in as $email", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }.onFailure {
            GoogleAuthManager.setOnboardingCompleted(context)
            isOnboardingOpen = false
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // Daily check: auto backup if not done yet today
            CloudBackupManager.autoBackupIfDailyDue(context, app.database)

            // Daily OTA update check
            val update = OtaUpdateManager.checkUpdateOnLaunchIfDue(context)
            if (update != null) {
                launchUpdateInfo = update
            }
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

        if (isOnboardingOpen) {
            OnboardingDialog(
                onSignInWithGoogle = {
                    onboardingSignInLauncher.launch(GoogleAuthManager.getSignInIntent(context))
                },
                onContinueOffline = {
                    GoogleAuthManager.setOnboardingCompleted(context)
                    isOnboardingOpen = false
                }
            )
        }

        launchUpdateInfo?.let { updateInfo ->
            SimpleUpdateDialog(
                releaseInfo = updateInfo,
                onConfirmUpdate = {
                    val url = updateInfo.downloadUrl ?: ""
                    launchUpdateInfo = null
                    OtaUpdateManager.startDownloadAndInstall(context, url, updateInfo.latestVersion)
                },
                onDismissToday = {
                    OtaUpdateManager.dismissUpdateForToday(context)
                    launchUpdateInfo = null
                }
            )
        }
    }
}
