package me.apomazkin.dictionarypicker.entity

import androidx.annotation.DrawableRes

data class DictUiEntity(
    val id: Long,
    @DrawableRes val flagRes: Int,
    val title: String,
    val numericCode: Int,
)
