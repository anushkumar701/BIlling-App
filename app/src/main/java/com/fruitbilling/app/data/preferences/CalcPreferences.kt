package com.fruitbilling.app.data.preferences

import android.content.Context

object CalcPreferences {
    private const val PREFS_NAME = "calc_display_prefs"
    private const val KEY_BUTTON_HEIGHT = "key_button_height_dp"

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
}
