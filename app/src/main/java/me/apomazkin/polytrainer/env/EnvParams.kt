package me.apomazkin.polytrainer.env

import me.apomazkin.polytrainer.BuildConfig
import javax.inject.Inject


interface EnvParams {
    val appVersion: String
    val isDebug: Boolean
}

class EnvParamsImpl @Inject constructor() : EnvParams {
    override val appVersion: String
        get() = BuildConfig.VERSION_NAME

    override val isDebug: Boolean
        get() = BuildConfig.DEBUG
}