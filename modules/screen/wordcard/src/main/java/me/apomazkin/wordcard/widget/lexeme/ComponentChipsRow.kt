@file:OptIn(ExperimentalLayoutApi::class)

package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formTextHint
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId

/**
 * Ряд chip'ов для добавления значений. IMAGE — вне скоупа.
 * IS486: видимость чипов — правило участия (явное поле [addableTypeIds],
 * пересчитывается reducer'ом): черновик — только ядра; зависимые открываются
 * по мере активации целей. CHOICE-чип открывает пикер опций (onAddChoice).
 */
@Composable
internal fun ComponentChipsRow(
    availableTypes: List<ComponentType>,
    addableTypeIds: Set<ComponentTypeId>,
    enabled: Boolean,
    onAddComponent: (ComponentTypeId) -> Unit,
    onAddChoice: (ComponentType) -> Unit,
) {
    val chips = availableTypes.filter {
        it.template != ComponentTemplate.IMAGE && it.id in addableTypeIds
    }
    if (chips.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.align(Alignment.End),
            text = stringResource(id = R.string.word_card_add_component_label),
            style = LexemeStyle.BodySBold.copy(letterSpacing = 0.2.sp),
            color = formTextHint,
        )
        Spacer(modifier = Modifier.height(8.dp))
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
                    onClick = {
                        if (type.template == ComponentTemplate.CHOICE) onAddChoice(type)
                        else onAddComponent(type.id)
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}
