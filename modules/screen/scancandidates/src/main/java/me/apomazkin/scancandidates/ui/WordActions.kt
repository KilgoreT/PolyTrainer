package me.apomazkin.scancandidates.ui

import me.apomazkin.scancandidates.mate.Msg
import me.apomazkin.scancandidates.mate.ScanWordUiItem

/**
 * Действия строки слова и кнопок карточки под ним. Тап превращается в
 * сообщение по видимому состоянию строки: карточка есть — свернуть или
 * раскрыть (AWP-42); слово «в словаре» без карточки — сохранённые (AWP-46);
 * иначе — запрос значений (AWP-43). Решений в composable нет.
 */
internal sealed interface WordAction {
    data class Tap(
        val item: ScanWordUiItem,
    ) : WordAction

    data class Delete(
        val text: String,
    ) : WordAction

    /** «Проверить по тексту» у слова «в словаре» (AWP-49). */
    data class CheckByText(
        val text: String,
    ) : WordAction

    /** «Повторить» на ошибке слова (AWP-61.7). */
    data class RetryLookup(
        val text: String,
    ) : WordAction

    data class Add(
        val text: String,
    ) : WordAction

    /** «Повторить» под «не добавилось» (AWP-26). */
    data class RetryAdd(
        val text: String,
    ) : WordAction

    /** Заголовок «Сопоставления» в карточке (AWP-31.4). */
    data class ToggleMatches(
        val text: String,
    ) : WordAction
}

internal fun WordAction.toMsg(): Msg = when (this) {
    is WordAction.Tap -> when {
        item.card != null -> Msg.ToggleCard(item.text)
        item.isInDictionary -> Msg.OpenSavedCard(item.text)
        else -> Msg.LoadLexemes(item.text)
    }
    is WordAction.Delete -> Msg.DeleteWord(text)
    is WordAction.CheckByText -> Msg.LoadLexemes(text)
    is WordAction.RetryLookup -> Msg.LoadLexemes(text)
    is WordAction.Add -> Msg.AddWord(text)
    is WordAction.RetryAdd -> Msg.RetryAdd(text)
    is WordAction.ToggleMatches -> Msg.ToggleComponentMatches(text)
}
