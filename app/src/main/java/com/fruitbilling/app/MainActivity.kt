package com.fruitbilling.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.fruitbilling.app.data.backup.CloudBackupManager
import com.fruitbilling.app.ui.navigation.AppNavigation
import com.fruitbilling.app.ui.theme.FruitBillingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FruitBillingApp

        handleIncomingBackupIntent(intent, app)

        setContent {
            FruitBillingTheme {
                AppNavigation(app = app)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val app = application as FruitBillingApp
        handleIncomingBackupIntent(intent, app)
    }

    private fun handleIncomingBackupIntent(intent: Intent?, app: FruitBillingApp) {
        val uri: Uri = intent?.data
            ?: intent?.getParcelableExtra(Intent.EXTRA_STREAM)
            ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val result = CloudBackupManager.restoreFromUri(this@MainActivity, uri, app.database)
            withContext(Dispatchers.Main) {
                result.onSuccess { stats ->
                    val msg = when {
                        stats.totalCount == 0 -> "Backup data is already up-to-date."
                        stats.billsRestored == 0 -> "Restored ${stats.productsRestored} fruits from backup!"
                        stats.productsRestored == 0 -> "Restored ${stats.billsRestored} bills from backup!"
                        else -> "Restored ${stats.productsRestored} fruits & ${stats.billsRestored} bills from backup!"
                    }
                    Toast.makeText(this@MainActivity, "✅ $msg", Toast.LENGTH_LONG).show()
                }.onFailure { error ->
                    Toast.makeText(
                        this@MainActivity,
                        "Could not restore backup: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}

