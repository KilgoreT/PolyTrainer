package me.apomazkin.langpicker.entity

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class LangPresetUi(
    @DrawableRes val flagRes: Int,
    val countryNumericCode: Int,
    @StringRes val langNameRes: Int,
)