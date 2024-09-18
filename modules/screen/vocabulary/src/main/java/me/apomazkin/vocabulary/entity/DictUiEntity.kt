package me.apomazkin.vocabulary.entity

import androidx.annotation.DrawableRes

data class DictUiEntity(
    @DrawableRes val flagRes: Int,
    val title: String,
    val numericCode: Int,
)
