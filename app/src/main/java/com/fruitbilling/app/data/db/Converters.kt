package com.fruitbilling.app.data.db

import androidx.room.TypeConverter
import com.fruitbilling.app.data.model.BillStatus
import com.fruitbilling.app.data.model.PaymentMethod
import com.fruitbilling.app.data.model.ProductUnit
import java.math.BigDecimal

class Converters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) }
    }

    @TypeConverter
    fun fromProductUnit(unit: ProductUnit): String {
        return unit.name
    }

    @TypeConverter
    fun toProductUnit(value: String): ProductUnit {
        return try {
            ProductUnit.valueOf(value)
        } catch (e: Exception) {
            ProductUnit.KG
        }
    }

    @TypeConverter
    fun fromBillStatus(status: BillStatus): String {
        return status.name
    }

    @TypeConverter
    fun toBillStatus(value: String): BillStatus {
        return try {
            BillStatus.valueOf(value)
        } catch (e: Exception) {
            BillStatus.ACTIVE
        }
    }

    @TypeConverter
    fun fromPaymentMethod(paymentMethod: PaymentMethod?): String? {
        return paymentMethod?.name
    }

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? {
        return value?.let {
            try {
                PaymentMethod.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
    }
}
