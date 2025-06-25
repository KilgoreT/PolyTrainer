@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.icondropdowned

import androidx.annotation.DrawableRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource


sealed interface IconSource {

    val tint: Color
    val contentDescription: StringSource?

    fun updateContentDescription(value: StringSource): IconSource

    @Composable
    fun Widget()

    companion object {
        private val DEFAULT_TINT = Color.Unspecified

        fun fromResId(
                @DrawableRes resId: Int,
                tint: Color = DEFAULT_TINT,
                contentDescription: StringSource? = null,
        ): IconFromResId = IconFromResId(
                resId = resId,
                tint = tint,
                contentDescription = contentDescription
        )

        fun fromVector(
                imageVector: ImageVector,
                tint: Color = DEFAULT_TINT,
                contentDescription: StringSource? = null,
        ): IconFromImageVector = IconFromImageVector(
                imageVector = imageVector,
                tint = tint,
                contentDescription = contentDescription
        )
    }

}

data class IconFromResId(
        @DrawableRes val resId: Int,
        override val tint: Color,
        override val contentDescription: StringSource?,
) : IconSource {
    override fun updateContentDescription(value: StringSource) = copy(
            contentDescription = value
    )

    @Composable
    override fun Widget() {
        Icon(
                painter = painterResource(id = resId),
                tint = tint,
                contentDescription = contentDescription?.asString(),
        )
    }
}

data class IconFromImageVector(
        val imageVector: ImageVector,
        override val tint: Color,
        override val contentDescription: StringSource?,
) : IconSource {

    override fun updateContentDescription(value: StringSource): IconSource = copy(
            contentDescription = value
    )

    @Composable
    override fun Widget() {
        Icon(
                imageVector = imageVector,
                contentDescription = contentDescription?.asString(),
                tint = tint
        )
    }
}