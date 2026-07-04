package me.apomazkin.component_widgets.widgets

import androidx.compose.runtime.Composable
import me.apomazkin.core_resources.R
import me.apomazkin.ui.btn.PrimaryLongFabWidget
import me.apomazkin.ui.preview.PreviewWidget

/**
 * IS481 phase 2: принимает structure из удалённых widget/CreateComponentFab.kt
 * (обоих screen-модулей; миграция).
 *
 * Wrapper поверх [PrimaryLongFabWidget] (Tier 1 primitive). Render 1-в-1 phase 1.
 */
@Composable
fun CreateComponentFab(
    onClick: () -> Unit,
) {
    PrimaryLongFabWidget(
        iconRes = R.drawable.ic_add,
        titleRes = R.string.components_create_cta,
        enabled = true,
        onClick = onClick,
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    CreateComponentFab(onClick = {})
}
