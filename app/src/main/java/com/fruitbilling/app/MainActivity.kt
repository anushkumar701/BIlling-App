package com.fruitbilling.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fruitbilling.app.ui.navigation.AppNavigation
import com.fruitbilling.app.ui.theme.FruitBillingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FruitBillingApp

        setContent {
            FruitBillingTheme {
                AppNavigation(app = app)
            }
        }
    }
}
