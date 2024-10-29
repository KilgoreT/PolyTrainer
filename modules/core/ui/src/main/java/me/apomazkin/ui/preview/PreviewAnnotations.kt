package me.apomazkin.ui.preview

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider

@Preview(
    showBackground = true,
    locale = "Ru",
)
@Preview(
    showBackground = true,
    locale = "En",
)
annotation class PreviewWidget(
    val group: String = "",
    val name: String = "",
)

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_3,
    locale = "Ru"
)
@Preview(
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_3,
    locale = "En"
)
annotation class PreviewScreen

class BoolParam : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> = sequenceOf(true, false)
}