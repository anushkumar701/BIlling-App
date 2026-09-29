package com.fruitbilling.app.data.preferences

import android.content.Context

object SurveyPreferences {

    private const val PREFS_NAME = "app_survey_prefs"
    private const val KEY_FIRST_INSTALL_TIME = "first_install_time"
    private const val KEY_SURVEY_COMPLETED = "survey_completed"
    private const val KEY_SURVEY_DISMISSED_TIME = "survey_dismissed_time"
    private const val KEY_LAST_RATING = "last_rating"
    private const val KEY_LAST_FEEDBACK = "last_feedback"
    private const val KEY_LAST_TAGS = "last_tags"

    /**
     * Initializes the install/first-launch timestamp if not already set.
     */
    fun recordAppLaunch(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_FIRST_INSTALL_TIME)) {
            prefs.edit()
                .putLong(KEY_FIRST_INSTALL_TIME, System.currentTimeMillis())
                .apply()
        }
    }

    fun getFirstInstallTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val time = prefs.getLong(KEY_FIRST_INSTALL_TIME, 0L)
        if (time == 0L) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_FIRST_INSTALL_TIME, now).apply()
            return now
        }
        return time
    }

    fun isSurveyCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SURVEY_COMPLETED, false)
    }

    fun getSurveyDismissedTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_SURVEY_DISMISSED_TIME, 0L)
    }

    /**
     * Prompts after 2 days (48 hours) of app usage if the survey hasn't been completed.
     * If user taps "Maybe Later", re-prompt after at least 3 days.
     */
    fun shouldPromptSurvey(context: Context): Boolean {
        if (isSurveyCompleted(context)) return false

        val now = System.currentTimeMillis()
        val installTime = getFirstInstallTime(context)
        val twoDaysMs = 2L * 24L * 60L * 60L * 1000L

        if (now - installTime < twoDaysMs) return false

        val dismissedTime = getSurveyDismissedTime(context)
        val threeDaysMs = 3L * 24L * 60L * 60L * 1000L
        if (dismissedTime > 0 && (now - dismissedTime < threeDaysMs)) return false

        return true
    }

    fun dismissSurvey(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_SURVEY_DISMISSED_TIME, System.currentTimeMillis())
            .apply()
    }

    fun submitSurvey(context: Context, rating: Int, tags: Set<String>, feedback: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SURVEY_COMPLETED, true)
            .putInt(KEY_LAST_RATING, rating)
            .putStringSet(KEY_LAST_TAGS, tags)
            .putString(KEY_LAST_FEEDBACK, feedback.trim())
            .apply()
    }
}
