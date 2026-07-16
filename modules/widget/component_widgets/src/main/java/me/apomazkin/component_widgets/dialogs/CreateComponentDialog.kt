@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package me.apomazkin.component_widgets.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.component_widgets.widgets.labelRes
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.NameError
import me.apomazkin.lexeme.Scope
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.ui.dialog.base.LexemeDialog

/**
 * IS481 phase 2: принимает structure из удалённых widget/CreateComponentDialog.kt
 * (обоих screen-модулей; миграция). Phase 2 расширение: scope_slot (Manager variant only).
 *
 * API rewrite: плоские примитивы вместо `CreateDialogState`.
 *
 * `hostVariant` enum управляет visibility scope_slot:
 * - [HostVariant.Manager] — рендерит scope radio + dictionary chip-list (multi-select).
 * - [HostVariant.PerDict] — scope_slot полностью скрыт (scope hardcoded в Reducer).
 *
 * `availableDictionaries: List<DictionaryRef>` — display-only DTO (host маппит
 * `DictionaryApiEntity` → `DictionaryRef`); избегаем dep `:core:core-db-api`.
 */
@Composable
fun CreateComponentDialog(
    name: String,
    template: ComponentTemplate,
    isMultiple: Boolean,
    scope: Scope,
    nameError: NameError?,
    isSubmitting: Boolean,
    availableDictionaries: List<DictionaryRef>,
    selectedDictionaryIds: Set<Long>,
    hostVariant: HostVariant,
    onNameChange: (String) -> Unit,
    onTemplateSelect: (ComponentTemplate) -> Unit,
    onMultiToggle: (Boolean) -> Unit,
    onScopeChange: (Scope) -> Unit,
    onDictionaryToggle: (Long) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val canSubmit = name.trim().isNotEmpty() &&
        nameError == null &&
        !isSubmitting &&
        when (scope) {
            Scope.Global -> true
            is Scope.PerDictionaries -> selectedDictionaryIds.isNotEmpty()
        }

    LexemeDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(id = R.string.components_create_dialog_title),
                style = LexemeStyle.H6,
                color = blackColor,
            )

            // Name section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ComponentDialogLabel(textRes = R.string.components_create_field_name, accent = true)
                ComponentNameField(value = name, onValueChange = onNameChange)
                nameError?.let { err ->
                    Text(
                        text = stringResource(id = err.labelRes()),
                        style = LexemeStyle.BodyS,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // Template radio-group
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ComponentDialogLabel(textRes = R.string.components_create_field_template, accent = false)
                ComponentTemplateRadioGroup(selected = template, onSelect = onTemplateSelect)
            }

            // Multi toggle row
            ComponentMultiToggle(
                textRes = R.string.components_create_field_is_multi,
                checked = isMultiple,
                onToggle = onMultiToggle,
            )

            // Scope picker — Manager variant only (phase 2 NEW)
            if (hostVariant == HostVariant.Manager) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ComponentDialogLabel(textRes = R.string.components_create_field_scope, accent = false)
                    ComponentRadioRow(
                        textRes = R.string.components_create_scope_global,
                        selected = scope is Scope.Global,
                        onClick = { onScopeChange(Scope.Global) },
                    )
                    ComponentRadioRow(
                        textRes = R.string.components_create_scope_per_dict,
                        selected = scope is Scope.PerDictionaries,
                        onClick = { onScopeChange(Scope.PerDictionaries(emptyList())) },
                    )
                    if (scope is Scope.PerDictionaries) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            availableDictionaries.forEach { dict ->
                                val selected = dict.id in selectedDictionaryIds
                                FilterChip(
                                    selected = selected,
                                    onClick = { onDictionaryToggle(dict.id) },
                                    label = { Text(text = dict.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        labelColor = blackColor,
                                        selectedLabelColor = Color.White,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            ComponentDialogActions(
                submitRes = R.string.components_button_create,
                submitEnabled = canSubmit,
                onCancel = onDismiss,
                onSubmit = onSubmit,
            )
        }
    }
}

/**
 * Host variant enum — управляет visibility scope_slot.
 * - [Manager] — render scope picker (radio + multi-dict chip-list).
 * - [PerDict] — scope_slot скрыт (scope hardcoded в Reducer к
 *   `Scope.PerDictionaries(listOf(dictId))`).
 */
enum class HostVariant { Manager, PerDict }

/**
 * Lightweight display-only DTO для multi-dict picker chip-list.
 * Хост маппит `DictionaryApiEntity` → `DictionaryRef` (id + name); избегаем dep
 * `:core:core-db-api` в shared widget module.
 */
data class DictionaryRef(
    val id: Long,
    val name: String,
)
