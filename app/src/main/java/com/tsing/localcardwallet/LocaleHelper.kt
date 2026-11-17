package com.tsing.localcardwallet

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** 应用语言：跟随系统 / 简体中文 / English */
object LocaleHelper {
    const val SYSTEM = "system"
    const val ZH = "zh"
    const val EN = "en"

    private const val PREF = "locale_prefs"
    private const val KEY = "app_locale"

    fun currentTag(context: Context): String =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, SYSTEM) ?: SYSTEM

    fun setTag(context: Context, tag: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
    }

    fun findActivity(context: Context): android.app.Activity? {
        var ctx: Context = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /** 在 attachBaseContext 里调用，按用户选择包装 Configuration */
    fun wrap(base: Context): Context {
        val tag = currentTag(base)
        if (tag == SYSTEM) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
