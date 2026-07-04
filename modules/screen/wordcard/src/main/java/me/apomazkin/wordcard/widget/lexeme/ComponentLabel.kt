package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.toRef

/** Лейбл из ref (snapshot ИЛИ живой type.toRef()). */
@Composable
internal fun labelOfRef(ref: ComponentTypeRef): String = when (ref) {
    is ComponentTypeRef.BuiltIn ->
        if (ref.key == BuiltInComponent.TRANSLATION) stringResource(id = R.string.word_card_bottom_translation)
        else ref.key.name
    is ComponentTypeRef.UserDefined -> ref.name
}

/**
 * A12: лейбл ЗНАЧЕНИЯ — приоритет живому типу из справочника по id, fallback на снимок ref
 * (окно до прихода ComponentTypesLoaded). Поле рендерится ВСЕГДА.
 */
@Composable
internal fun componentValueLabel(
    componentTypeId: ComponentTypeId,
    snapshotRef: ComponentTypeRef,
    availableTypes: List<ComponentType>,
): String {
    val live = availableTypes.firstOrNull { it.id == componentTypeId }
    return labelOfRef(live?.toRef() ?: snapshotRef)
}

/** Лейбл для chip'а в ChipsRow — там всегда есть живой ComponentType. */
@Composable
internal fun componentLabelOf(type: ComponentType): String = labelOfRef(type.toRef())
