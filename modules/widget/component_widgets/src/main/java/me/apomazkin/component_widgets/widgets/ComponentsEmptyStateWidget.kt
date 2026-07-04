package me.apomazkin.component_widgets.widgets

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.ui.btn.PrimaryFullButtonWidget
import me.apomazkin.ui.preview.PreviewWidget

/**
 * IS481 phase 2: принимает structure из удалённых widget/ComponentsEmptyStateWidget.kt
 * (обоих screen-модулей; миграция components_manager + per_dictionary_components).
 *
 * Render 1-в-1 phase 1. Variant выбирается callsite через resIds
 * (см. `ui_layout.md` § ComponentsEmptyStateWidget § behavior).
 */
@Composable
fun ComponentsEmptyStateWidget(
    @StringRes headlineRes: Int,
    @StringRes bodyRes: Int,
    onCreate: () -> Unit,
    @StringRes ctaRes: Int = R.string.components_empty_cta,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier.size(64.dp),
            painter = painterResource(id = R.drawable.ic_components),
            tint = blackColor,
            contentDescription = null,
        )
        Text(
            text = stringResource(id = headlineRes),
            style = LexemeStyle.H6,
            color = blackColor,
        )
        Text(
            text = stringResource(id = bodyRes),
            style = LexemeStyle.BodyL,
            color = blackColor,
        )
        PrimaryFullButtonWidget(
            titleRes = ctaRes,
            enabled = true,
            onClick = onCreate,
        )
    }
}

@Composable
@PreviewWidget
private fun Preview() {
    ComponentsEmptyStateWidget(
        headlineRes = R.string.components_empty_headline_manager,
        bodyRes = R.string.components_empty_body_manager,
        onCreate = {},
    )
}
