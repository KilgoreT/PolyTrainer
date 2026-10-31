package me.apomazkin.scancandidates.ui

import me.apomazkin.scancandidates.mate.Msg
import me.apomazkin.scancandidates.mate.ScanWordUiItem

/**
 * Действия строки слова, кнопок под ним и кнопок плашек нейронки. Тап
 * превращается в сообщение по видимому состоянию строки: карточка есть —
 * свернуть или раскрыть (AWP-42); слово «в словаре» без карточки — плашки
 * словаря (AWP-46); иначе — запрос значений (AWP-43). Решений в composable
 * нет.
 */
internal sealed interface WordAction {
    data class Tap(
        val item: ScanWordUiItem,
    ) : WordAction

    /** «Убрать из списка» у слова (AWP-48). */
    data class Remove(
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

    /** «Создать» или «Перенести» у плашки (AWP-20); какая — решает «Куда» плашки в стейте. */
    data class Write(
        val text: String,
        val key: String,
    ) : WordAction

    /** «Повторить» под плашкой после «не добавилось» (AWP-26). */
    data class RetryAdd(
        val text: String,
        val key: String,
    ) : WordAction

    /** «Не нужно» у плашки (AWP-64). */
    data class Dismiss(
        val text: String,
        val key: String,
    ) : WordAction

    /** Заголовок «Сопоставления» под плашками слова (AWP-31.4). */
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
    is WordAction.Remove -> Msg.RemoveWord(text)
    is WordAction.CheckByText -> Msg.LoadLexemes(text)
    is WordAction.RetryLookup -> Msg.LoadLexemes(text)
    is WordAction.Write -> Msg.AddLexeme(text, key)
    is WordAction.RetryAdd -> Msg.RetryAdd(text, key)
    is WordAction.Dismiss -> Msg.DismissLexeme(text, key)
    is WordAction.ToggleMatches -> Msg.ToggleComponentMatches(text)
}
