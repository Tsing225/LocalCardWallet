package com.tsing.localcardwallet

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 应用设置。Compose 状态驱动，改动会立即重组界面。 */
object AppSettings {
    private const val PREF_NAME = "app_settings"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_BIOMETRIC = "biometric_pan"
    private const val KEY_DYNAMIC = "dynamic_color"

    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    private var prefs: SharedPreferences? = null

    var themeMode by mutableStateOf(THEME_SYSTEM)
        private set
    var biometricForPan by mutableStateOf(true)
        private set
    var dynamicColor by mutableStateOf(false)
        private set

    fun init(context: Context) {
        if (prefs == null) {
            val p = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs = p
            themeMode = p.getInt(KEY_THEME, THEME_SYSTEM)
            biometricForPan = p.getBoolean(KEY_BIOMETRIC, true)
            dynamicColor = p.getBoolean(KEY_DYNAMIC, false)
        }
    }

    fun setThemeMode(context: Context, mode: Int) {
        themeMode = mode
        prefs(context).edit().putInt(KEY_THEME, mode).apply()
    }

    fun setBiometricForPan(context: Context, enabled: Boolean) {
        biometricForPan = enabled
        prefs(context).edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    fun setDynamicColor(context: Context, enabled: Boolean) {
        dynamicColor = enabled
        prefs(context).edit().putBoolean(KEY_DYNAMIC, enabled).apply()
    }

    private fun prefs(context: Context): SharedPreferences {
        prefs?.let { return it }
        val p = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs = p
        return p
    }
}
