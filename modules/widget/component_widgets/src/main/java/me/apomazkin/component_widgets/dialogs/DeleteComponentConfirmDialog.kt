package me.apomazkin.component_widgets.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.ui.dialog.base.LexemeDialog

/**
 * IS481 phase 2: принимает structure из удалённых widget/DeleteComponentConfirmDialog.kt
 * (обоих screen-модулей; миграция).
 *
 * API rewrite: плоские примитивы + lightweight [DeletionImpactRef] display-only DTO
 * (Dependency Rule: shared widget не coupled на full `DeletionImpact` domain).
 *
 * 3-way conditional: loading | impact != null | unavailable.
 */
@Composable
fun DeleteComponentConfirmDialog(
    name: String,
    impact: DeletionImpactRef?,
    isLoadingImpact: Boolean,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val canConfirm = !isLoadingImpact && !isSubmitting

    LexemeDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(
                    id = R.string.components_delete_dialog_title,
                    name,
                ),
                style = LexemeStyle.H6,
                color = blackColor,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    isLoadingImpact && impact == null -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }

                    impact != null -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (impact.valueCount > 0) {
                                Text(
                                    text = stringResource(
                                        id = R.string.components_delete_impact_values,
                                        impact.valueCount,
                                    ),
                                    style = LexemeStyle.BodyL,
                                    color = blackColor,
                                )
                            }
                            if (impact.dictCount > 0) {
                                Text(
                                    text = stringResource(
                                        id = R.string.components_delete_impact_dicts,
                                        impact.dictCount,
                                    ),
                                    style = LexemeStyle.BodyL,
                                    color = blackColor,
                                )
                            }
                            if (impact.quizCount > 0) {
                                Text(
                                    text = stringResource(
                                        id = R.string.components_delete_impact_quiz,
                                        impact.quizCount,
                                    ),
                                    style = LexemeStyle.BodyL,
                                    color = blackColor,
                                )
                            }
                            if (impact.prefsCount > 0) {
                                Text(
                                    text = stringResource(
                                        id = R.string.components_delete_impact_prefs,
                                        impact.prefsCount,
                                    ),
                                    style = LexemeStyle.BodyL,
                                    color = blackColor,
                                )
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = stringResource(id = R.string.components_delete_impact_unavailable),
                            style = LexemeStyle.BodyL,
                            color = blackColor,
                        )
                    }
                }
            }

            Text(
                text = stringResource(id = R.string.components_delete_hint),
                style = LexemeStyle.BodyS,
                color = blackColor,
            )

            ComponentDialogActions(
                submitRes = R.string.button_delete,
                submitEnabled = canConfirm,
                onCancel = onDismiss,
                onSubmit = onConfirm,
                destructive = true,
            )
        }
    }
}

/**
 * Lightweight display-only DTO для delete-confirm impact preview.
 * Хост маппит `DeletionImpact` → `DeletionImpactRef` (counts only — избегаем coupling
 * shared widget на domain entity).
 */
data class DeletionImpactRef(
    val valueCount: Int,
    val dictCount: Int,
    val quizCount: Int,
    val prefsCount: Int,
)
