package com.universe_st.quickwriter.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    const val CODE_SYSTEM = "system"
    const val CODE_EN = "en"
    const val CODE_ZH_CN = "zh-rCN"
    const val CODE_ZH_TW = "zh-rTW"

    fun applyLocale(activity: Activity, languageCode: String): Boolean {
        val locale = when (languageCode) {
            CODE_EN -> Locale.ENGLISH
            CODE_ZH_CN -> Locale.SIMPLIFIED_CHINESE
            CODE_ZH_TW -> Locale.TRADITIONAL_CHINESE
            else -> null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (locale != null) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            }
        } else {
            @Suppress("DEPRECATION")
            applyConfigurationLegacy(activity, locale)
        }

        return true
    }

    fun wrapContextForLocale(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            CODE_EN -> Locale.ENGLISH
            CODE_ZH_CN -> Locale.SIMPLIFIED_CHINESE
            CODE_ZH_TW -> Locale.TRADITIONAL_CHINESE
            else -> return context
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context
        }

        @Suppress("DEPRECATION")
        val config = Configuration(context.resources.configuration)
        Locale.setDefault(locale)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    @Suppress("DEPRECATION")
    private fun applyConfigurationLegacy(activity: Activity, locale: Locale?) {
        val config = Configuration(activity.resources.configuration)
        if (locale != null) {
            Locale.setDefault(locale)
            config.setLocale(locale)
        } else {
            Locale.setDefault(Locale.getDefault())
            config.setLocale(Locale.getDefault())
        }
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
    }

    fun getDisplayLanguageCode(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }

        return when {
            locale.language.equals("zh", ignoreCase = true) && locale.country.equals("TW", ignoreCase = true) -> CODE_ZH_TW
            locale.language.equals("zh", ignoreCase = true) && locale.country.equals("HK", ignoreCase = true) -> CODE_ZH_TW
            locale.language.equals("zh", ignoreCase = true) -> CODE_ZH_CN
            locale.language.equals("en", ignoreCase = true) -> CODE_EN
            else -> CODE_SYSTEM
        }
    }

    fun languageCodeToResourceId(code: String): Int {
        return when (code) {
            CODE_SYSTEM -> com.universe_st.quickwriter.R.string.language_system
            CODE_EN -> com.universe_st.quickwriter.R.string.language_en
            CODE_ZH_CN -> com.universe_st.quickwriter.R.string.language_zh_cn
            CODE_ZH_TW -> com.universe_st.quickwriter.R.string.language_zh_tw
            else -> com.universe_st.quickwriter.R.string.language_system
        }
    }
}
