package me.apomazkin.prefs

/**
 * Per-(тип квиза × словарь) pref-ключ набора групп тренировки.
 *
 * Single source of truth — используется в `QuizGroupSelectionStore`
 * (чтение/запись/подписка). Значение — id групп через запятую;
 * отсутствие ключа = «Все». Изоляция выборов между типами квизов и
 * словарями — через оба измерения в ключе.
 *
 * Прежний ключ одиночной группы (`quiz_group_<тип>_dict_<id>`) не
 * читается: после перехода на набор выбор начинается с «Все».
 */
fun quizGroupsPrefKey(quizType: String, dictionaryId: Long): String =
    "quiz_groups_${quizType}_dict_$dictionaryId"
