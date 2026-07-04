@file:OptIn(ExperimentalLayoutApi::class)

package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId

/**
 * Ряд chip'ов для добавления значений. P3: только TEXT-шаблоны (image — вне скоупа).
 * Non-multi уже добавленные (в addedNonMultipleTypeIds) скрыты; multi всегда видим.
 */
@Composable
internal fun ComponentChipsRow(
    availableTypes: List<ComponentType>,
    addedNonMultipleTypeIds: Set<ComponentTypeId>,
    enabled: Boolean,
    onAddComponent: (ComponentTypeId) -> Unit,
) {
    val chips = availableTypes.filter {
        it.template == ComponentTemplate.TEXT && it.id !in addedNonMultipleTypeIds
    }
    if (chips.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.33f).align(Alignment.End),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chips.forEach { type ->
                SubentityChip(
                    label = componentLabelOf(type),
                    iconRes = R.drawable.ic_add,
                    enabled = enabled,
                    onClick = { onAddComponent(type.id) },
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}
