package me.apomazkin.candidates.ui

import me.apomazkin.candidates.PhotoAddFailure
import me.apomazkin.candidates.PhotoComponent
import me.apomazkin.candidates.PhotoDataKind
import me.apomazkin.candidates.PhotoDictionaryForm
import me.apomazkin.candidates.PhotoNotAddedReason
import me.apomazkin.candidates.PhotoPartOfSpeechOption
import me.apomazkin.candidates.PhotoTarget
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.core_resources.R
import java.util.Locale

/*
 * IS532: подписи окна добавления — имена компонентов и частей речи теми же
 * ресурсами, что у карточки слова; счёт «N значений в M слов» формами
 * plurals.
 */

/** Имя компонента: встроенный — ресурсом, свой — как назвал пользователь. */
@Composable
internal fun componentLabel(component: PhotoComponent): String = when (component.systemKey) {
    BuiltInComponent.TRANSLATION -> stringResource(id = R.string.word_card_bottom_translation)
    BuiltInComponent.PART_OF_SPEECH -> stringResource(id = R.string.builtin_component_part_of_speech)
    BuiltInComponent.EXAMPLE -> stringResource(id = R.string.builtin_component_example)
    null -> component.name.orEmpty()
}

/** Имя цели вида данных: компонент в кавычках или «не добавлять». */
@Composable
internal fun targetLabel(target: PhotoTarget, form: PhotoDictionaryForm): String = when (target) {
    is PhotoTarget.Component -> form.components.firstOrNull { it.id == target.componentId }?.let { "«${componentLabel(it)}»" }
        ?: stringResource(id = R.string.photo_add_mapping_none)
    PhotoTarget.None -> stringResource(id = R.string.photo_add_mapping_none)
}

/** Подпись строки соответствий: перевод — с языком перевода словаря (AWP, набросок §2). */
@Composable
internal fun kindLabel(kind: PhotoDataKind, translationLanguage: String): String = when (kind) {
    PhotoDataKind.TRANSLATION -> stringResource(id = R.string.photo_add_mapping_translation, languageName(translationLanguage))
    PhotoDataKind.GLOSS -> stringResource(id = R.string.photo_add_mapping_gloss)
    PhotoDataKind.EXAMPLES -> stringResource(id = R.string.photo_add_mapping_examples)
    PhotoDataKind.PART_OF_SPEECH -> stringResource(id = R.string.photo_add_mapping_part_of_speech)
}

/** Название языка по тегу в текущей локали: «русский», «English». */
internal fun languageName(tag: String): String = Locale.forLanguageTag(tag).getDisplayLanguage(Locale.getDefault()).ifEmpty { tag }

/** Полное название опции «Части речи» (список выбора). */
@Composable
internal fun partOfSpeechLabel(option: PhotoPartOfSpeechOption): String {
    option.label?.let { return it }
    val res = when (option.key) {
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
    return if (res != null) stringResource(id = res) else option.key.orEmpty()
}

/** Короткая помета опции у строки лексемы («гл.», «сущ.»). */
@Composable
internal fun partOfSpeechShortLabel(option: PhotoPartOfSpeechOption): String {
    option.label?.let { return it }
    val res = when (option.key) {
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
    return if (res != null) stringResource(id = res) else partOfSpeechLabel(option)
}

/** «17 значений в 4 слова» — кнопка, вопрос (AWP-13). */
@Composable
internal fun countInWords(lexemes: Int, words: Int): String = stringResource(
    id = R.string.photo_add_count_in,
    pluralStringResource(id = R.plurals.photo_add_lexemes, count = lexemes, lexemes),
    pluralStringResource(id = R.plurals.photo_add_words, count = words, words),
)

@Composable
internal fun lexemesCount(count: Int): String = pluralStringResource(id = R.plurals.photo_add_lexemes, count = count, count)

@Composable
internal fun notAddedReasonLabel(reason: PhotoNotAddedReason): String = when (reason) {
    PhotoNotAddedReason.NOT_FOUND -> stringResource(id = R.string.photo_add_not_found)
    PhotoNotAddedReason.ERROR -> stringResource(id = R.string.photo_add_error)
    PhotoNotAddedReason.NO_LEXEMES -> stringResource(id = R.string.photo_add_no_lexemes)
}

/** Текст «не добавилось» с причиной сброса (AWP-21.3…21.6). */
@Composable
internal fun failureLabel(failure: PhotoAddFailure): String {
    val reason = when (failure) {
        is PhotoAddFailure.GroupMissing -> stringResource(id = R.string.photo_add_failed_group)
        is PhotoAddFailure.WordMissing -> stringResource(id = R.string.photo_add_failed_word)
        is PhotoAddFailure.TypeUnavailable -> stringResource(id = R.string.photo_add_failed_type)
        PhotoAddFailure.NoCoreValue -> stringResource(id = R.string.photo_add_failed_core)
        PhotoAddFailure.DictionaryMissing -> stringResource(id = R.string.photo_add_dictionary_missing)
        PhotoAddFailure.Failed -> null
    }
    val base = stringResource(id = R.string.photo_add_failed)
    return if (reason == null) base else "$base. $reason"
}
