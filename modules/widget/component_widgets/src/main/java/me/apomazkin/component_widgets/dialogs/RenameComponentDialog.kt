package me.apomazkin.component_widgets.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.component_widgets.widgets.labelRes
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.NameError
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.ui.btn.CancelButtonWidget
import me.apomazkin.ui.btn.PrimaryFullButtonWidget
import me.apomazkin.ui.dialog.base.LexemeDialog
import me.apomazkin.ui.input.base.LexemeTextFieldWidget

/**
 * IS481 phase 2: принимает structure из удалённых widget/RenameComponentDialog.kt
 * (обоих screen-модулей; миграция).
 *
 * API rewrite: плоские примитивы вместо целого `RenameDialogState` (Dependency Rule:
 * shared widget не coupled на screen-specific state shape).
 *
 * `canSubmit` вычисляется внутри composable из плоских параметров.
 */
@Composable
fun RenameComponentDialog(
    originalName: String,
    editedName: String,
    nameError: NameError?,
    isSubmitting: Boolean,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val canSubmit = editedName.isNotBlank() &&
        editedName != originalName &&
        nameError == null &&
        !isSubmitting

    LexemeDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(id = R.string.components_rename_dialog_title),
                style = LexemeStyle.H6,
                color = blackColor,
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = R.string.components_rename_original),
                    style = LexemeStyle.BodyS,
                    color = blackColor,
                )
                Text(
                    text = originalName,
                    style = LexemeStyle.BodyL,
                    color = blackColor,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = R.string.components_rename_new_name),
                    style = LexemeStyle.BodyS,
                    color = blackColor,
                )
                LexemeTextFieldWidget(
                    modifier = Modifier.fillMaxWidth(),
                    value = editedName,
                    placeHolder = null,
                    onValueChange = onNameChange,
                    onKeyboardActions = {},
                )
                nameError?.let { err ->
                    Text(
                        text = stringResource(id = err.labelRes()),
                        style = LexemeStyle.BodyS,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CancelButtonWidget(
                    modifier = Modifier.weight(1f),
                    height = 56.dp,
                    onClick = onDismiss,
                )
                PrimaryFullButtonWidget(
                    modifier = Modifier.weight(1f),
                    titleRes = R.string.components_button_save,
                    enabled = canSubmit,
                    onClick = onSubmit,
                )
            }
        }
    }
}
