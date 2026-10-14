package me.apomazkin.candidates

/**
 * Что сделать со словом диалога карточки при записи, если его значения
 * взяты у леммы-формы (WCL-15): переименовать в лемму или перенести всё
 * в существующее слово словаря с текстом леммы и удалить. Выбор один на
 * диалог, по умолчанию его нет; [lemma] — текст леммы-формы.
 */
sealed interface LemmaWordChoice {
    val lemma: String

    /** Слово диалога получает текст леммы; одноимённое слово словаря, если есть, остаётся — дубль допускается. */
    data class Rename(
        override val lemma: String,
    ) : LemmaWordChoice

    /** Выбранные значения и свои лексемы слова диалога уходят в слово [wordId] словаря, слово диалога удаляется. */
    data class Transfer(
        override val lemma: String,
        val wordId: Long,
    ) : LemmaWordChoice
}
