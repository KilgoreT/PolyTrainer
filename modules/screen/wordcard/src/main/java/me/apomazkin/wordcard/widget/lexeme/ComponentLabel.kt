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
internal fun componentLabelOf(type: ComponentType): String = when {
    // IS486: builtin «Часть речи» — display из общего ресурса.
    type.systemKey == BuiltInComponent.PART_OF_SPEECH ->
        stringResource(id = R.string.builtin_component_part_of_speech)
    else -> labelOfRef(type.toRef())
}

/**
 * IS486: display-лейбл опции CHOICE — label-override ?: ресурс по systemKey ?: ключ.
 * Ключи builtin-опций части речи локализуются ресурсами (spec §4).
 */
@Composable
internal fun optionDisplayLabel(option: me.apomazkin.lexeme.ComponentOption): String {
    option.label?.let { return it }
    val res = when (option.systemKey) {
        "noun" -> R.string.part_of_speech_option_noun
        "verb" -> R.string.part_of_speech_option_verb
        "adjective" -> R.string.part_of_speech_option_adjective
        "adverb" -> R.string.part_of_speech_option_adverb
        "preposition" -> R.string.part_of_speech_option_preposition
        "phrase" -> R.string.part_of_speech_option_phrase
        else -> null
    }
    return if (res != null) stringResource(id = res) else option.systemKey.orEmpty()
}
