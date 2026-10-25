package com.fruitbilling.app.data.preferences

import android.content.Context
import com.fruitbilling.app.data.model.BillingMode

object CalcPreferences {
    private const val PREFS_NAME = "calc_display_prefs"
    private const val KEY_BUTTON_HEIGHT = "key_button_height_dp"
    private const val KEY_BILLING_MODE = "saved_billing_mode"

    const val DEFAULT_HEIGHT_DP = 50f
    const val COMPACT_HEIGHT_DP = 42f
    const val LARGE_HEIGHT_DP = 60f

    fun getKeypadHeightDp(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_BUTTON_HEIGHT, DEFAULT_HEIGHT_DP)
    }

    fun setKeypadHeightDp(context: Context, heightDp: Float) {
        val clamped = heightDp.coerceIn(38f, 65f)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_BUTTON_HEIGHT, clamped)
            .apply()
    }

    fun getSavedBillingMode(context: Context): BillingMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeStr = prefs.getString(KEY_BILLING_MODE, BillingMode.EXPERIENCED.name)
        return try {
            BillingMode.valueOf(modeStr ?: BillingMode.EXPERIENCED.name)
        } catch (_: Exception) {
            BillingMode.EXPERIENCED
        }
    }

    fun setSavedBillingMode(context: Context, mode: BillingMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BILLING_MODE, mode.name)
            .apply()
    }
}

