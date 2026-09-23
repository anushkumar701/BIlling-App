package com.fruitbilling.app.data.model

enum class ProductUnit(val displayName: String, val unitLabel: String) {
    KG("kg", "/kg"),
    PIECE("piece", "/pc")
}

enum class BillStatus {
    ACTIVE,
    HELD,
    COMPLETED
}

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    UPI("UPI"),
    PENDING("Pending")
}

enum class BillingMode(val displayName: String) {
    EXPERIENCED("Experienced"),
    BEGINNER("Beginner")
}
