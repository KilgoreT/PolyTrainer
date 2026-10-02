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
internal fun labelOfRef(ref: ComponentTypeRef): String =
    when (ref) {
        is ComponentTypeRef.BuiltIn ->
            when (ref.key) {
                BuiltInComponent.TRANSLATION -> stringResource(id = R.string.word_card_bottom_translation)
                BuiltInComponent.PART_OF_SPEECH -> stringResource(id = R.string.builtin_component_part_of_speech)
                BuiltInComponent.EXAMPLE -> stringResource(id = R.string.builtin_component_example) // IS491
            }
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
        "pronoun" -> R.string.part_of_speech_option_pronoun
        "numeral" -> R.string.part_of_speech_option_numeral
        "preposition" -> R.string.part_of_speech_option_preposition
        "interjection" -> R.string.part_of_speech_option_interjection
        "phrasal_verb" -> R.string.part_of_speech_option_phrasal_verb
        "collocation" -> R.string.part_of_speech_option_collocation
        "idiom" -> R.string.part_of_speech_option_idiom
        "phrase" -> R.string.part_of_speech_option_phrase
        else -> null
    }
    return if (res != null) stringResource(id = res) else option.systemKey.orEmpty()
}

/**
 * Короткая подпись опции для чипа выбранного значения: у builtin-опций
 * «Части речи» — словарная помета («колл.», «фраз. гл.»), те же ресурсы,
 * что у чипа вопроса квиз-чата. В списке выбора — полные названия
 * ([optionDisplayLabel]). Пользовательская опция — её текст как есть.
 */
@Composable
internal fun optionShortLabel(option: me.apomazkin.lexeme.ComponentOption): String {
    option.label?.let { return it }
    val res = when (option.systemKey) {
        "noun" -> R.string.part_of_speech_short_noun
        "verb" -> R.string.part_of_speech_short_verb
        "adjective" -> R.string.part_of_speech_short_adjective
        "adverb" -> R.string.part_of_speech_short_adverb
        "pronoun" -> R.string.part_of_speech_short_pronoun
        "numeral" -> R.string.part_of_speech_short_numeral
        "preposition" -> R.string.part_of_speech_short_preposition
        "interjection" -> R.string.part_of_speech_short_interjection
        "phrasal_verb" -> R.string.part_of_speech_short_phrasal_verb
        "collocation" -> R.string.part_of_speech_short_collocation
        "idiom" -> R.string.part_of_speech_short_idiom
        "phrase" -> R.string.part_of_speech_short_phrase
        else -> null
    }
    return if (res != null) stringResource(id = res) else optionDisplayLabel(option)
}
