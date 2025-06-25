package me.apomazkin.icondropdowned

import androidx.annotation.StringRes
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle

sealed interface StringSource {

    val style: TextStyle?
    val color: Color?

    @Composable
    fun textStyle() = style ?: LocalTextStyle.current

    @Composable
    fun asString(): String

    companion object {

        fun fromRes(
                @StringRes resId: Int,
                style: TextStyle? = null,
                color: Color? = null,
        ) = Res(
                resId = resId,
                style = style,
                color = color,
        )

        fun fromRaw(
                value: String,
                style: TextStyle? = null,
                color: Color? = null,
        ) = Raw(
                value = value,
                style = style,
                color = color
        )
    }
}

data class Res(
        @StringRes val resId: Int,
        override val style: TextStyle?,
        override val color: Color?,
) : StringSource {
    @Composable
    override fun asString(): String = stringResource(id = resId)
}

data class Raw(
        val value: String,
        override val style: TextStyle?,
        override val color: Color?,
) : StringSource {
    @Composable
    override fun asString(): String = value
}