package com.gelengeden.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists the user-selected app appearance and exposes it for immediate Compose updates. */
class AppearanceManager(context: Context) {

    enum class ThemeMode {
        SYSTEM,
        LIGHT,
        DARK
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(readThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private fun readThemeMode(): ThemeMode = runCatching {
        ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name).orEmpty())
    }.getOrDefault(ThemeMode.SYSTEM)

    companion object {
        private const val PREFS_NAME = "gelengeden_appearance"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
