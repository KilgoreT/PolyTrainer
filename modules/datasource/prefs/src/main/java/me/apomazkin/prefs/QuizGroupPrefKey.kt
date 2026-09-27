package me.apomazkin.prefs

/**
 * IS500. Per-(тип квиза × словарь) pref-ключ выбора группы квиза.
 *
 * Single source of truth — используется в `QuizGroupSelectionStore`
 * (чтение/запись/подписка). Значение — `groupId` строкой; отсутствие
 * ключа = «Все». Изоляция выборов между типами квизов и словарями —
 * через оба измерения в ключе.
 */
fun quizGroupPrefKey(quizType: String, dictionaryId: Long): String =
    "quiz_group_${quizType}_dict_$dictionaryId"
