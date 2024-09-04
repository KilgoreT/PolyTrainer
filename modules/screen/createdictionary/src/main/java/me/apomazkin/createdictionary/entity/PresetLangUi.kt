package me.apomazkin.createdictionary.entity

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class PresetLangUi(
    @DrawableRes val flagRes: Int,
    val countryNumericCode: Int,
    @StringRes val langNameRes: Int,
)