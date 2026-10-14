package me.apomazkin.polytrainer.env

import me.apomazkin.polytrainer.BuildConfig
import javax.inject.Inject


interface EnvParams {
    val appVersion: String
    val isDebug: Boolean

    /** В сборке есть ключ сервера слов — входы в словарь (камера, значения в карточке) видны (WCL-08). */
    val hasServerAccess: Boolean
}

class EnvParamsImpl @Inject constructor() : EnvParams {
    override val appVersion: String
        get() = BuildConfig.VERSION_NAME

    override val isDebug: Boolean
        get() = BuildConfig.DEBUG

    override val hasServerAccess: Boolean
        get() = BuildConfig.LEXEME_ACCESS_KEY.isNotBlank()
}