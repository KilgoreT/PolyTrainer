package me.apomazkin.component_widgets.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.component_widgets.widgets.CardinalityDowngradePreviewWidget
import me.apomazkin.component_widgets.widgets.labelRes
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.ui.btn.CancelButtonWidget
import me.apomazkin.ui.btn.PrimaryFullButtonWidget
import me.apomazkin.ui.dialog.base.LexemeDialog
import me.apomazkin.ui.dropdown.LexemeRadioRow
import me.apomazkin.ui.input.base.LexemeTextFieldWidget

/**
 * IS481 phase 2 — NEW edit dialog. Mounted на обоих screen-модулях.
 *
 * Structure: title → name (label + LexemeTextFieldWidget + error) → template
 * (label + LexemeRadioRow × ComponentTemplate.entries; clickable, gating на UseCase) →
 * multi (Checkbox + label) → preview_slot (CardinalityDowngradePreviewWidget iff
 * preview != null) → actions.
 *
 * API: плоский (без coupling на mate `EditDialogState`); host raskl'аdyvaet state на
 * примитивы. `nameErrorRes` — resolved StringRes (mate `EditNameError` маппится на
 * host'е либо через ext function).
 *
 * `canSubmit = name.trim().isNotBlank() && nameErrorRes == null && !isSubmitting &&
 *              (dirty: name/template/isMultiple changed)`.
 */
@Composable
fun EditComponentDialog(
    name: String,
    template: ComponentTemplate,
    isMultiple: Boolean,
    originalName: String,
    originalTemplate: ComponentTemplate,
    originalIsMultiple: Boolean,
    @StringRes nameErrorRes: Int?,
    previewInlineIds: List<Long>?,
    previewTotalCount: Int,
    previewShowAllVisible: Boolean,
    lexemeLabel: (Long) -> String,
    isSubmitting: Boolean,
    onNameChange: (String) -> Unit,
    onTemplateSelect: (ComponentTemplate) -> Unit,
    onMultiToggle: (Boolean) -> Unit,
    onShowAllImpacted: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dirty = name != originalName ||
        template != originalTemplate ||
        isMultiple != originalIsMultiple
    val canSubmit = name.trim().isNotBlank() &&
        nameErrorRes == null &&
        !isSubmitting &&
        dirty

    LexemeDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(id = R.string.components_edit_dialog_title),
                style = LexemeStyle.H6,
                color = blackColor,
            )

            // Name section
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = R.string.components_edit_field_name),
                    style = LexemeStyle.BodyS,
                    color = blackColor,
                )
                LexemeTextFieldWidget(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    placeHolder = null,
                    onValueChange = onNameChange,
                    onKeyboardActions = {},
                )
                nameErrorRes?.let { res ->
                    Text(
                        text = stringResource(id = res),
                        style = LexemeStyle.BodyS,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // Template radio-group (clickable; UseCase enforces immutability)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = R.string.components_edit_field_template),
                    style = LexemeStyle.BodyS,
                    color = blackColor,
                )
                ComponentTemplate.entries.forEach { t ->
                    LexemeRadioRow(
                        textRes = t.labelRes(),
                        selected = t == template,
                        onClick = { onTemplateSelect(t) },
                        color = blackColor,
                    )
                }
            }

            // Multi toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isMultiple,
                    onCheckedChange = onMultiToggle,
                )
                Text(
                    text = stringResource(id = R.string.components_edit_field_is_multi),
                    style = LexemeStyle.BodyL,
                    color = blackColor,
                )
            }

            // Preview slot — cardinality downgrade blocked
            if (previewInlineIds != null) {
                CardinalityDowngradePreviewWidget(
                    inlineIds = previewInlineIds,
                    totalCount = previewTotalCount,
                    showAllVisible = previewShowAllVisible,
                    lexemeLabel = lexemeLabel,
                    onShowAll = onShowAllImpacted,
                )
            }

            // Actions
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
