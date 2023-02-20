package me.apomazkin.main.entity

import androidx.annotation.DrawableRes

data class LangUiEntity(
    @DrawableRes val iconRes: Int,
    val title: String,
    val numericCode: Int,
)
