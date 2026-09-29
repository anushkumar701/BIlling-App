package com.fruitbilling.app.data.preferences

import android.content.Context

object ShopPreferences {

    private const val PREFS_NAME = "shop_profile_prefs"
    private const val KEY_SHOP_NAME = "shop_name"
    private const val KEY_SHOP_PHONE = "shop_phone"
    private const val KEY_SHOP_ADDRESS = "shop_address"

    private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
    private const val KEY_CURRENCY_CODE = "currency_code"
    private const val KEY_APP_LANGUAGE = "app_language"
    private const val KEY_LANGUAGE_SELECTED = "language_selected"

    const val DEFAULT_SHOP_NAME = "Retail Store"
    const val DEFAULT_CURRENCY_SYMBOL = "₹"
    const val DEFAULT_CURRENCY_CODE = "INR"
    const val DEFAULT_APP_LANGUAGE = "en"

    fun getShopName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_SHOP_NAME, null)?.trim()
        return if (!name.isNullOrBlank()) name else DEFAULT_SHOP_NAME
    }

    fun setShopName(context: Context, name: String) {
        val clean = name.trim().ifBlank { DEFAULT_SHOP_NAME }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SHOP_NAME, clean)
            .apply()
    }

    fun getShopPhone(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SHOP_PHONE, null)?.trim()?.takeIf { it.isNotBlank() }
    }

    fun setShopPhone(context: Context, phone: String?) {
        val clean = phone?.trim()?.takeIf { it.isNotBlank() }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SHOP_PHONE, clean)
            .apply()
    }

    fun getShopAddress(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SHOP_ADDRESS, null)?.trim()?.takeIf { it.isNotBlank() }
    }

    fun setShopAddress(context: Context, address: String?) {
        val clean = address?.trim()?.takeIf { it.isNotBlank() }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SHOP_ADDRESS, clean)
            .apply()
    }

    fun getCurrencySymbol(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENCY_SYMBOL, DEFAULT_CURRENCY_SYMBOL) ?: DEFAULT_CURRENCY_SYMBOL
    }

    fun setCurrency(context: Context, symbol: String, code: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CURRENCY_SYMBOL, symbol.trim())
            .putString(KEY_CURRENCY_CODE, code.trim().uppercase())
            .apply()
    }

    fun getCurrencyCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENCY_CODE, DEFAULT_CURRENCY_CODE) ?: DEFAULT_CURRENCY_CODE
    }

    fun getAppLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_APP_LANGUAGE, DEFAULT_APP_LANGUAGE) ?: DEFAULT_APP_LANGUAGE
    }

    fun setAppLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_LANGUAGE, languageCode.trim())
            .putBoolean(KEY_LANGUAGE_SELECTED, true)
            .apply()
    }

    fun isLanguageSelected(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LANGUAGE_SELECTED, false)
    }
}
