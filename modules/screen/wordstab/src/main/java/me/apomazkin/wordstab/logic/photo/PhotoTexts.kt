package me.apomazkin.wordstab.logic.photo

/*
 * IS532: русские формы счёта для кнопки, вопроса и сообщения
 * (AWP-13, AWP-23): «3 значения в 2 слова», «1 значение в 1 слово»,
 * «17 значений в 4 слова».
 */

/** «N значений в M слов» — хвост кнопки, вопроса и сообщения. */
internal fun countPhrase(lexemes: Int, words: Int): String =
    "${plural(lexemes, "значение", "значения", "значений")} в ${plural(words, "слово", "слова", "слов")}"

/** Сообщение после записи (AWP-23.1). */
internal fun addedMessage(lexemes: Int, words: Int): String = "Добавлено ${countPhrase(lexemes, words)}"

/** Русская форма по числу: 1 — одна, 2–4 — вторая, остальное — третья; 11–14 — третья. */
internal fun plural(count: Int, one: String, few: String, many: String): String {
    val mod100 = count % 100
    val mod10 = count % 10
    val form = when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
    return "$count $form"
}
