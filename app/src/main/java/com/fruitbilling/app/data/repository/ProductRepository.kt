package com.fruitbilling.app.data.repository

import com.fruitbilling.app.data.db.dao.ProductDao
import com.fruitbilling.app.data.model.Product
import com.fruitbilling.app.data.model.ProductUnit
import com.fruitbilling.app.util.ProductImageUtils
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

    suspend fun insertProduct(
        name: String,
        price: BigDecimal,
        unit: ProductUnit,
        iconRef: String? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
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
            unit = unit,
            iconRef = iconRef
        )
        val id = productDao.insertProduct(product)
        triggerCloudSync()
        Result.success(id)
    }

    suspend fun updateProduct(
        id: Long,
        name: String,
        price: BigDecimal,
        unit: ProductUnit,
        iconRef: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
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

        // If replacing image with a different one, delete the old image
        if (existing.iconRef != null && existing.iconRef != iconRef) {
            ProductImageUtils.deleteImage(existing.iconRef)
        }

        val updated = existing.copy(
            name = trimmed,
            price = price,
            unit = unit,
            iconRef = iconRef,
            updatedAt = System.currentTimeMillis()
        )
        productDao.updateProduct(updated)
        triggerCloudSync()
        Result.success(Unit)
    }

    suspend fun deleteProduct(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = productDao.getProductById(id)
        if (existing?.iconRef != null) {
            ProductImageUtils.deleteImage(existing.iconRef)
        }
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

    companion object {
        val PROFESSIONAL_DEFAULT_PRODUCTS = listOf(
            Product(name = "Apple", price = BigDecimal("160"), unit = ProductUnit.KG),
            Product(name = "Banana", price = BigDecimal("50"), unit = ProductUnit.KG),
            Product(name = "Orange", price = BigDecimal("100"), unit = ProductUnit.KG),
            Product(name = "Pomegranate", price = BigDecimal("150"), unit = ProductUnit.KG),
            Product(name = "Grapes", price = BigDecimal("90"), unit = ProductUnit.KG),
            Product(name = "Papaya", price = BigDecimal("50"), unit = ProductUnit.KG),
            Product(name = "Watermelon", price = BigDecimal("35"), unit = ProductUnit.KG),
            Product(name = "Mango", price = BigDecimal("180"), unit = ProductUnit.KG),
            Product(name = "Pineapple", price = BigDecimal("60"), unit = ProductUnit.PIECE),
            Product(name = "Guava", price = BigDecimal("60"), unit = ProductUnit.KG),
            Product(name = "Sweet Lime (Mosambi)", price = BigDecimal("80"), unit = ProductUnit.KG),
            Product(name = "Kiwi", price = BigDecimal("35"), unit = ProductUnit.PIECE)
        )
    }

    suspend fun clearAllProducts() = withContext(Dispatchers.IO) {
        val all = productDao.getAllProductsSync()
        all.forEach { p ->
            if (p.iconRef != null) {
                ProductImageUtils.deleteImage(p.iconRef)
            }
        }
        productDao.deleteAllProducts()
        triggerCloudSync()
    }

    suspend fun loadProfessionalDefaults() = withContext(Dispatchers.IO) {
        productDao.deleteAllProducts()
        productDao.insertAll(PROFESSIONAL_DEFAULT_PRODUCTS)
        triggerCloudSync()
    }

    suspend fun ensureDefaultProducts() = withContext(Dispatchers.IO) {
        val existing = productDao.getAllProductsSync()
        // If legacy informal products exist (e.g. Small apple, Pacha palam), upgrade to professional items
        val hasLegacyInformal = existing.any { 
            it.name.contains("Small apple", ignoreCase = true) || 
            it.name.contains("Pacha palam", ignoreCase = true) ||
            it.name.contains("Manja", ignoreCase = true) ||
            it.name.contains("Sevvazai", ignoreCase = true)
        }

        if (hasLegacyInformal) {
            productDao.deleteAllProducts()
            productDao.insertAll(PROFESSIONAL_DEFAULT_PRODUCTS)
            triggerCloudSync()
        } else if (existing.isEmpty()) {
            val prefs = com.fruitbilling.app.FruitBillingApp.instance.getSharedPreferences("catalog_prefs", android.content.Context.MODE_PRIVATE)
            val alreadyInitialized = prefs.getBoolean("has_initialized_catalog_v103", false)
            if (!alreadyInitialized) {
                prefs.edit().putBoolean("has_initialized_catalog_v103", true).apply()
                productDao.insertAll(PROFESSIONAL_DEFAULT_PRODUCTS)
                triggerCloudSync()
            }
        }
    }
}
