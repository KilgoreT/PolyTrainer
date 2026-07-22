package me.apomazkin.component_widgets.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.core_resources.R

/**
 * IS486: display-имя компонента по system_key builtin'а (пословарные builtin
 * хранят name = null — имя локализуется ресурсом; spec §4). Для user-defined
 * ([systemKey] == null) возвращает [name].
 */
@Composable
fun componentDisplayName(systemKey: String?, name: String): String = when (systemKey) {
    "translation" -> stringResource(id = R.string.word_card_bottom_translation)
    "part_of_speech" -> stringResource(id = R.string.builtin_component_part_of_speech)
    null -> name
    else -> systemKey
}

/**
 * IS486: display-лейбл опции CHOICE — label-override ?: ресурс по systemKey ?: ключ
 * (ключи builtin-опций части речи локализуются ресурсами; spec §4).
 */
@Composable
fun optionDisplayLabel(systemKey: String?, label: String?): String {
    label?.let { return it }
    val res = when (systemKey) {
        "noun" -> R.string.part_of_speech_option_noun
        "verb" -> R.string.part_of_speech_option_verb
        "adjective" -> R.string.part_of_speech_option_adjective
        "adverb" -> R.string.part_of_speech_option_adverb
        "preposition" -> R.string.part_of_speech_option_preposition
        "phrase" -> R.string.part_of_speech_option_phrase
        else -> null
    }
    return if (res != null) stringResource(id = res) else systemKey.orEmpty()
}
