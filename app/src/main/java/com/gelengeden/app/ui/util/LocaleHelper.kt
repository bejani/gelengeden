package com.gelengeden.app.ui.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * App language for Iranian users: Persian (Farsi) + Iran region.
 * Applied at Application and Activity level so resources, layout direction (RTL),
 * and number/date formatting stay consistent.
 */
object LocaleHelper {
    val APP_LOCALE: Locale = Locale.Builder()
        .setLanguage("fa")
        .setRegion("IR")
        .build()

    fun wrap(context: Context): Context {
        val config = Configuration(context.resources.configuration)
        Locale.setDefault(APP_LOCALE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(APP_LOCALE))
        } else {
            @Suppress("DEPRECATION")
            config.locale = APP_LOCALE
        }
        config.setLayoutDirection(APP_LOCALE)
        return context.createConfigurationContext(config)
    }
}
