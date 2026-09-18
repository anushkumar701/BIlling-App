package com.fruitbilling.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fruitbilling.app.data.db.dao.BillDao
import com.fruitbilling.app.data.db.dao.BillItemDao
import com.fruitbilling.app.data.db.dao.ProductDao
import com.fruitbilling.app.data.model.Bill
import com.fruitbilling.app.data.model.BillItem
import com.fruitbilling.app.data.model.Product
import com.fruitbilling.app.data.model.ProductUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import androidx.room.migration.Migration

@Database(
    entities = [Product::class, Bill::class, BillItem::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun billDao(): BillDao
    abstract fun billItemDao(): BillItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS bill_items_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        billId INTEGER NOT NULL,
                        expression TEXT NOT NULL DEFAULT '',
                        calculatedAmount TEXT NOT NULL DEFAULT '0',
                        productId INTEGER,
                        productNameSnapshot TEXT,
                        unitPriceSnapshot TEXT,
                        unit TEXT,
                        quantityOrWeight TEXT,
                        normalizedWeight TEXT,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(billId) REFERENCES bills(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO bill_items_new (id, billId, expression, calculatedAmount, productId, productNameSnapshot, unitPriceSnapshot, unit, quantityOrWeight, normalizedWeight, createdAt)
                    SELECT id, billId, (COALESCE(productNameSnapshot, '') || ' ' || COALESCE(quantityOrWeight, '')), calculatedAmount, productId, productNameSnapshot, unitPriceSnapshot, unit, quantityOrWeight, normalizedWeight, 0
                    FROM bill_items
                """.trimIndent())

                db.execSQL("DROP TABLE bill_items")
                db.execSQL("ALTER TABLE bill_items_new RENAME TO bill_items")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bill_items_billId ON bill_items(billId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bill_items_productId ON bill_items(productId)")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fruit_billing_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDefaultProducts(database.productDao())
                    }
                }
            }

            suspend fun populateDefaultProducts(productDao: ProductDao) {
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
}
