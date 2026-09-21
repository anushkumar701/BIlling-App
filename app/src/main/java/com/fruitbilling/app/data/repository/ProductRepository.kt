package com.fruitbilling.app.data.repository

import com.fruitbilling.app.data.db.dao.ProductDao
import com.fruitbilling.app.data.model.Product
import com.fruitbilling.app.data.model.ProductUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class ProductRepository(private val productDao: ProductDao) {

    val activeProducts: Flow<List<Product>> = productDao.getActiveProducts()
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    suspend fun getProductById(id: Long): Product? = withContext(Dispatchers.IO) {
        productDao.getProductById(id)
    }

    suspend fun isNameUnique(name: String, excludeId: Long = 0): Boolean = withContext(Dispatchers.IO) {
        val existing = if (excludeId > 0) {
            productDao.getProductByNameExcludingId(name, excludeId)
        } else {
            productDao.getProductByName(name)
        }
        existing == null
    }

    private fun triggerCloudSync() {
        try {
            com.fruitbilling.app.data.backup.CloudBackupManager.triggerAsyncCloudSync(
                context = com.fruitbilling.app.FruitBillingApp.instance,
                database = com.fruitbilling.app.FruitBillingApp.instance.database
            )
        } catch (_: Exception) {}
    }

    suspend fun insertProduct(name: String, price: BigDecimal, unit: ProductUnit): Result<Long> = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Product name cannot be empty."))
        }
        if (price <= BigDecimal.ZERO) {
            return@withContext Result.failure(IllegalArgumentException("Price must be greater than 0."))
        }
        if (!isNameUnique(trimmed)) {
            return@withContext Result.failure(IllegalArgumentException("Product name already exists."))
        }

        val product = Product(
            name = trimmed,
            price = price,
            unit = unit
        )
        val id = productDao.insertProduct(product)
        triggerCloudSync()
        Result.success(id)
    }

    suspend fun updateProduct(id: Long, name: String, price: BigDecimal, unit: ProductUnit): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Product name cannot be empty."))
        }
        if (price <= BigDecimal.ZERO) {
            return@withContext Result.failure(IllegalArgumentException("Price must be greater than 0."))
        }
        if (!isNameUnique(trimmed, excludeId = id)) {
            return@withContext Result.failure(IllegalArgumentException("Product name already exists."))
        }

        val existing = productDao.getProductById(id)
            ?: return@withContext Result.failure(IllegalArgumentException("Product not found."))

        val updated = existing.copy(
            name = trimmed,
            price = price,
            unit = unit,
            updatedAt = System.currentTimeMillis()
        )
        productDao.updateProduct(updated)
        triggerCloudSync()
        Result.success(Unit)
    }

    suspend fun deleteProduct(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        productDao.deleteProductById(id)
        triggerCloudSync()
        Result.success(Unit)
    }

    /**
     * Deactivate/reactivate a product (§8) without touching its row otherwise.
     * An inactive product drops out of billing (getActiveProducts) but its price/name
     * live on unchanged in any historical bill, exactly like a full delete would --
     * this is just the reversible option alongside it.
     */
    suspend fun setActive(id: Long, active: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = productDao.getProductById(id)
            ?: return@withContext Result.failure(IllegalArgumentException("Product not found."))
        productDao.updateProduct(existing.copy(active = active, updatedAt = System.currentTimeMillis()))
        triggerCloudSync()
        Result.success(Unit)
    }

    suspend fun ensureDefaultProducts() = withContext(Dispatchers.IO) {
        if (productDao.getProductCount() == 0) {
            val defaultProducts = listOf(
                Product(name = "Papali", price = BigDecimal("70"), unit = ProductUnit.KG),
                Product(name = "Manja / Poovam pazham", price = BigDecimal("80"), unit = ProductUnit.KG),
                Product(name = "Pacha palam", price = BigDecimal("40"), unit = ProductUnit.KG),
                Product(name = "Orange", price = BigDecimal("220"), unit = ProductUnit.KG),
                Product(name = "Madhulai", price = BigDecimal("120"), unit = ProductUnit.KG),
                Product(name = "Sevvazai", price = BigDecimal("120"), unit = ProductUnit.KG),
                Product(name = "Malapazham", price = BigDecimal("160"), unit = ProductUnit.KG),
                Product(name = "Small apple", price = BigDecimal("100"), unit = ProductUnit.KG),
                Product(name = "Big apple", price = BigDecimal("300"), unit = ProductUnit.KG),
                Product(name = "Box Apple", price = BigDecimal("200"), unit = ProductUnit.KG),
                Product(name = "Yelaki", price = BigDecimal("120"), unit = ProductUnit.KG),
                Product(name = "Karupurvalli", price = BigDecimal("80"), unit = ProductUnit.KG)
            )
            productDao.insertAll(defaultProducts)
        }
    }
}
