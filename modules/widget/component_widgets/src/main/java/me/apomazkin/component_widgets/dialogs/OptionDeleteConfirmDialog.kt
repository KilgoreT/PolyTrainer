package me.apomazkin.component_widgets.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import androidx.compose.ui.res.stringResource
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.theme.templateChipText
import me.apomazkin.ui.dialog.base.LexemeDialog

/**
 * IS486 (В2): confirm удаления опции CHOICE с impact-превью (spec К3–К5):
 * прямые значения-выборы + значения поддеревьев зависимых + число деградирующих
 * компонентов. Рендерится ПОВЕРХ Edit-диалога (вложенный конфирм).
 */
@Composable
fun OptionDeleteConfirmDialog(
    label: String,
    valueCount: Int?,
    descendantValueCount: Int?,
    degradedCount: Int?,
    isLoadingImpact: Boolean,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LexemeDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(id = R.string.components_option_delete_title),
                style = LexemeStyle.H6,
                color = blackColor,
            )
            Text(
                text = stringResource(id = R.string.components_option_delete_message, label),
                style = LexemeStyle.BodyM,
                color = templateChipText,
            )
            if (isLoadingImpact) {
                CircularProgressIndicator()
            } else {
                val totalValues = (valueCount ?: 0) + (descendantValueCount ?: 0)
                if (totalValues > 0) {
                    Text(
                        text = stringResource(id = R.string.components_option_delete_values, totalValues),
                        style = LexemeStyle.BodyM,
                        color = templateChipText,
                    )
                }
                if ((degradedCount ?: 0) > 0) {
                    Text(
                        text = stringResource(
                            id = R.string.components_option_delete_degraded,
                            degradedCount ?: 0,
                        ),
                        style = LexemeStyle.BodyM,
                        color = templateChipText,
                    )
                }
            }
            ComponentDialogActions(
                submitRes = R.string.button_delete,
                submitEnabled = !isLoadingImpact && !isSubmitting,
                onCancel = onDismiss,
                onSubmit = onConfirm,
                destructive = true,
            )
        }
    }
}
