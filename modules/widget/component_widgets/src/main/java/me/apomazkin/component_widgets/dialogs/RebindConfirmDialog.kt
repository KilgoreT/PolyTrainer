package me.apomazkin.component_widgets.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.theme.templateChipText
import me.apomazkin.ui.dialog.base.LexemeDialog

/**
 * IS486 умный сброс (решение 2026-07-21): конфирм перепривязки цели — обязателен
 * при любой смене зависимости/ядра. Показывает dry-run: «безопасно» (нулевой
 * impact) либо «будет скрыто N значений» (сумма своих + потомков).
 * Рендерится ПОВЕРХ Edit-диалога.
 */
@Composable
fun RebindConfirmDialog(
    valueCount: Int?,
    descendantValueCount: Int?,
    isLoadingImpact: Boolean,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LexemeDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(id = R.string.components_rebind_title),
                style = LexemeStyle.H6,
                color = blackColor,
            )
            if (isLoadingImpact) {
                CircularProgressIndicator()
            } else {
                val total = (valueCount ?: 0) + (descendantValueCount ?: 0)
                Text(
                    text = if (total == 0) {
                        stringResource(id = R.string.components_rebind_safe)
                    } else {
                        stringResource(id = R.string.components_rebind_values, total)
                    },
                    style = LexemeStyle.BodyM,
                    color = templateChipText,
                )
            }
            ComponentDialogActions(
                submitRes = R.string.components_button_save,
                submitEnabled = !isLoadingImpact && !isSubmitting,
                onCancel = onDismiss,
                onSubmit = onConfirm,
                destructive = ((valueCount ?: 0) + (descendantValueCount ?: 0)) > 0,
            )
        }
    }
}
