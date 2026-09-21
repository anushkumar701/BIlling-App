package com.fruitbilling.app

import android.app.Application
import com.fruitbilling.app.data.db.AppDatabase
import com.fruitbilling.app.data.repository.BillRepository
import com.fruitbilling.app.data.repository.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FruitBillingApp : Application() {

    companion object {
        lateinit var instance: FruitBillingApp
            private set
    }

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val productRepository by lazy { ProductRepository(database.productDao()) }
    val billRepository by lazy {
        BillRepository(
            database = database,
            billDao = database.billDao(),
            billItemDao = database.billItemDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Ensure default products exist on first launch & check daily backup (§17)
        applicationScope.launch(Dispatchers.IO) {
            productRepository.ensureDefaultProducts()
            com.fruitbilling.app.data.backup.CloudBackupManager.autoBackupIfDailyDue(this@FruitBillingApp, database)
        }
    }
}
