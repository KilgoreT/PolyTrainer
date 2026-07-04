package me.apomazkin.component_widgets.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeStyle

/**
 * IS481 phase 2 — inline cardinality downgrade preview. NEW widget.
 *
 * API (плоские примитивы, без coupling на mate `ImpactedLexemesPreview` sealed):
 * - `inlineIds` — top-3 (или все если ≤3) ids для inline отрисовки.
 * - `totalCount` — общее количество impacted lexemes (для drill-in label).
 * - `showAllVisible` — `true` если drill-in кнопка должна рендериться (totalCount > inlineIds.size).
 * - `lexemeLabel` — host-supplied resolver `(Long) -> String` (caller знает domain).
 * - `onShowAll` — drill-in callback (visible iff `showAllVisible`).
 *
 * Render: Column(padding=8, spacing=8, background=errorContainer, shape=rounded-12).
 * Title + inline rows + drill-in btn (conditional).
 */
@Composable
fun CardinalityDowngradePreviewWidget(
    inlineIds: List<Long>,
    totalCount: Int,
    showAllVisible: Boolean,
    lexemeLabel: (Long) -> String,
    onShowAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color = MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(id = R.string.components_edit_cardinality_blocked_title),
            style = LexemeStyle.BodyLBold.copy(
                color = MaterialTheme.colorScheme.onErrorContainer,
            ),
        )
        inlineIds.forEach { id ->
            Text(
                text = lexemeLabel(id),
                style = LexemeStyle.BodyM.copy(
                    color = MaterialTheme.colorScheme.onErrorContainer,
                ),
            )
        }
        if (showAllVisible) {
            TextButton(onClick = onShowAll) {
                Text(
                    text = stringResource(
                        id = R.string.components_edit_show_all,
                        totalCount,
                    ),
                    style = LexemeStyle.BodyLBold.copy(
                        color = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }
}
