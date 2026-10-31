package me.apomazkin.candidates.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.ComponentMatchLineUiItem
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.FormComponentUi
import me.apomazkin.candidates.FormOptionUi
import me.apomazkin.candidates.RemarkUiItem
import me.apomazkin.candidates.componentOf
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import java.util.Locale

/*
 * IS532/IS536: подписи окна и диалога. Имена встроенных компонентов и опций
 * — теми же ресурсами, что у карточки слова, имена своих — из формы; тексты
 * замечаний и строк сопоставления собираются из ресурсов по виду строки.
 */

/** Сколько первых значений слова показать в подписи переноса (WCL-15.1). */
private const val PREVIEW_VALUES = 3

/** Имя компонента: встроенный — ресурсом, свой — как назвал пользователь. */
@Composable
internal fun componentLabel(component: FormComponentUi): String = when (component.systemKey) {
    BuiltInComponent.TRANSLATION -> stringResource(id = R.string.word_card_bottom_translation)
    BuiltInComponent.PART_OF_SPEECH ->
        stringResource(id = R.string.builtin_component_part_of_speech)
    BuiltInComponent.EXAMPLE -> stringResource(id = R.string.builtin_component_example)
    null -> component.name.orEmpty()
}

/** Имя компонента по id; пусто, если компонента нет в форме. */
@Composable
internal fun componentLabel(
    form: DictionaryFormUi,
    componentId: Long,
): String = form.componentOf(componentId)?.let { componentLabel(it) }.orEmpty()

/** Подпись опции «Выбора»: своя — как назвал пользователь, встроенная — ресурсом по ключу. */
@Composable
internal fun optionLabel(option: FormOptionUi): String {
    option.label?.let { return it }
    val res = optionLabelRes(option.key)
    return if (res != null) stringResource(id = res) else option.key.orEmpty()
}

/** Подпись опции по id среди опций всех «Выборов» формы; пусто, если опции нет. */
@Composable
internal fun optionLabel(
    form: DictionaryFormUi,
    optionId: Long,
): String = form.options.values
    .flatten()
    .firstOrNull { it.id == optionId }
    ?.let { optionLabel(it) }
    .orEmpty()

/** Ресурс названия встроенной опции «Части речи» по ключу (AWP-17.4); null — ключ не встроенный. */
@StringRes
internal fun optionLabelRes(key: String?): Int? = when (key) {
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

/** Шаблон компонента словами: текст, выбор, текст с подписью, картинка (AWP-30). */
@Composable
internal fun templateLabel(template: ComponentTemplate): String = stringResource(
    id = when (template) {
        ComponentTemplate.TEXT -> R.string.candidates_template_text
        ComponentTemplate.CHOICE -> R.string.candidates_template_choice
        ComponentTemplate.CAPTIONED_TEXT -> R.string.candidates_template_captioned_text
        ComponentTemplate.IMAGE -> R.string.candidates_template_image
    },
)

/** Название языка по тегу в текущей локали: «русский», «English». */
internal fun languageName(tag: String): String =
    Locale.forLanguageTag(tag).getDisplayLanguage(Locale.getDefault()).ifEmpty { tag }

/**
 * Текст строки раздела «Замечания» по её виду (AWP-30); null — строки нет:
 * компонента из замечания нет в форме.
 */
@Composable
internal fun remarkText(
    remark: RemarkUiItem,
    form: DictionaryFormUi,
): String? = when (remark) {
    is RemarkUiItem.ComponentUnclear -> form.componentOf(remark.componentId)?.let { component ->
        stringResource(id = R.string.candidates_remark_component_unclear, componentLabel(component))
    }
    is RemarkUiItem.NoTranslation -> pluralStringResource(
        id = R.plurals.candidates_remark_no_translation,
        count = remark.count,
        remark.count,
        languageName(remark.translationLanguage),
    )
    is RemarkUiItem.ComponentRecommended -> stringResource(
        id = R.string.candidates_remark_recommended,
        remark.name,
        recommendedDescription(remark),
    )
}

/** «текст» или «выбор, опции: м.р., ж.р.» — что в скобках рекомендации. */
@Composable
private fun recommendedDescription(remark: RemarkUiItem.ComponentRecommended): String {
    val template = templateLabel(remark.template)
    if (remark.options.isEmpty()) return template
    return stringResource(
        id = R.string.candidates_template_with_options,
        template,
        remark.options.joinToString(", "),
    )
}

/**
 * Текст строки раздела «Сопоставление» по её виду (AWP-31): «поле → компонент»
 * или «компонент: опция».
 */
@Composable
internal fun matchLineText(
    line: ComponentMatchLineUiItem,
    form: DictionaryFormUi,
): String = when (line) {
    is ComponentMatchLineUiItem.FieldToComponent -> stringResource(
        id = R.string.candidates_match_field,
        line.field,
        componentLabel(form, line.componentId),
    )
    is ComponentMatchLineUiItem.ComponentOption -> stringResource(
        id = R.string.candidates_match_option,
        componentLabel(form, line.componentId),
        optionLabel(form, line.optionId),
    )
}

/** «17 значений в 4 слова» — кнопка и вопрос окна (AWP-13). */
@Composable
internal fun countInWords(
    lexemes: Int,
    words: Int,
): String = stringResource(
    id = R.string.photo_add_count_in,
    pluralStringResource(id = R.plurals.photo_add_lexemes, count = lexemes, lexemes),
    pluralStringResource(id = R.plurals.photo_add_words, count = words, words),
)

/** «3 значения» — кнопка диалога карточки (WCL-10). */
@Composable
internal fun lexemesCount(count: Int): String =
    pluralStringResource(id = R.plurals.photo_add_lexemes, count = count, count)

/** Текст «не добавилось» с причиной сброса (AWP-21.3…21.6). */
@Composable
fun failureLabel(failure: AddFailureUi): String {
    val reason = when (failure) {
        is AddFailureUi.GroupMissing -> stringResource(id = R.string.photo_add_failed_group)
        is AddFailureUi.WordMissing -> stringResource(id = R.string.photo_add_failed_word)
        is AddFailureUi.TypeUnavailable -> stringResource(id = R.string.photo_add_failed_type)
        AddFailureUi.NoCoreValue -> stringResource(id = R.string.photo_add_failed_core)
        AddFailureUi.DictionaryMissing -> stringResource(id = R.string.photo_add_dictionary_missing)
        AddFailureUi.Failed -> null
    }
    val base = stringResource(id = R.string.photo_add_failed)
    return if (reason == null) base else "$base. $reason"
}

/**
 * Первые значения слова для подписи переноса (WCL-15.1): до трёх через «, »,
 * дальше «…»; null — у слова значений нет.
 */
internal fun valuesPreview(values: List<String>): String? = when {
    values.isEmpty() -> null
    values.size > PREVIEW_VALUES -> values.take(PREVIEW_VALUES).joinToString(", ") + "…"
    else -> values.joinToString(", ")
}
