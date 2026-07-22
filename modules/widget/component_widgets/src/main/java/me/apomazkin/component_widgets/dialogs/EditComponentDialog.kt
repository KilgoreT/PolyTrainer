package me.apomazkin.component_widgets.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.component_widgets.widgets.CardinalityDowngradePreviewWidget
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.ui.dialog.base.LexemeDialog

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
    // ===== IS486: пикер цели (В1) — рендерится если targetItems != null (PerDict) =====
    targetItems: List<TargetPickerItem>? = null,
    selectedTarget: DependencyTarget = DependencyTarget.Lexeme,
    core: Boolean = true,
    onTargetSelect: (DependencyTarget) -> Unit = {},
    onCoreToggle: (Boolean) -> Unit = {},
    // ===== IS486: варианты CHOICE (В2). Существующие: rename на Submit, удаление
    // немедленно (конфирм у host'а); новые черновики: add на Submit =====
    existingOptions: List<Pair<Long, String>> = emptyList(),
    newOptionDrafts: List<String> = emptyList(),
    onOptionLabelChange: (Long, String) -> Unit = { _, _ -> },
    onOptionDeleteClick: (Long) -> Unit = {},
    onOptionDraftAdd: () -> Unit = {},
    onOptionDraftChange: (Int, String) -> Unit = { _, _ -> },
    onOptionDraftRemove: (Int) -> Unit = {},
    /** IS486: доп. dirty от host'а (цель/ядро/опции изменились). */
    extraDirty: Boolean = false,
) {
    val dirty = name != originalName ||
        template != originalTemplate ||
        isMultiple != originalIsMultiple ||
        extraDirty
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ComponentDialogLabel(textRes = R.string.components_edit_field_name, accent = true)
                ComponentNameField(value = name, onValueChange = onNameChange)
                nameErrorRes?.let { res ->
                    Text(
                        text = stringResource(id = res),
                        style = LexemeStyle.BodyS,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // Template radio-group (clickable; UseCase enforces immutability)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ComponentDialogLabel(textRes = R.string.components_edit_field_template, accent = false)
                ComponentTemplateRadioGroup(selected = template, onSelect = onTemplateSelect)
            }

            // IS486 (В2): варианты CHOICE.
            if (template == ComponentTemplate.CHOICE) {
                OptionListEditor(
                    existing = existingOptions,
                    drafts = newOptionDrafts,
                    showError = false,
                    onExistingChange = onOptionLabelChange,
                    onExistingDelete = onOptionDeleteClick,
                    onDraftChange = onOptionDraftChange,
                    onDraftRemove = onOptionDraftRemove,
                    onDraftAdd = onOptionDraftAdd,
                )
            }

            // Multi toggle row (IS486: для CHOICE мульти запрещён — spec §7.5)
            if (template != ComponentTemplate.CHOICE) {
                ComponentMultiToggle(
                    textRes = R.string.components_edit_field_is_multi,
                    checked = isMultiple,
                    onToggle = onMultiToggle,
                )
            }

            // IS486 (В1): пикер цели зависимости + галка «Ядро» (PerDict host).
            if (targetItems != null) {
                ComponentTargetPicker(
                    items = targetItems,
                    selected = selectedTarget,
                    core = core,
                    onTargetSelect = onTargetSelect,
                    onCoreToggle = onCoreToggle,
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

            ComponentDialogActions(
                submitRes = R.string.components_button_save,
                submitEnabled = canSubmit,
                onCancel = onDismiss,
                onSubmit = onSubmit,
            )
        }
    }
}
