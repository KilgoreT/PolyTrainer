package me.apomazkin.ui.preview

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    showBackground = true
)
annotation class PreviewWidget

@Preview(
    showBackground = true,
    locale = "Ru"
)
annotation class PreviewWidgetRu

@Preview(
    showBackground = true,
    locale = "En"
)
annotation class PreviewWidgetEn

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_3,
)
annotation class PreviewScreen

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_3,
    locale = "Ru"
)
annotation class PreviewScreenRu

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_3,
    locale = "En"
)
annotation class PreviewScreenEn