package me.apomazkin.quiztab.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.quiz.QuizGroupCount

/**
 * IS500. Данные пикера группы: текущий словарь, живые счётчики,
 * персист выбора. Валидацию выбора таб делает сам — доменной воронкой
 * `resolveQuizGroupState` на каждом эмите combine.
 */
interface QuizTabUseCase {

    /** Текущий словарь (fallback на первый; null — «словарей нет»). */
    fun flowCurrentDictId(): Flow<Long?>

    /** Живые группы словаря со счётчиком слов уровня 1. */
    fun flowQuizGroupCounts(dictionaryId: Long): Flow<List<QuizGroupCount>>

    /** Живой счётчик уровня 1 для пункта «Все». */
    fun flowDictionaryQuizWordCount(dictionaryId: Long): Flow<Int>

    /** Сырой pref-поток выбора группы (null = «Все»; без валидации). */
    fun flowGroupSelection(quizType: String, dictionaryId: Long): Flow<Long?>

    /** Персист выбора; null («Все») стирает ключ. */
    suspend fun setGroupSelection(quizType: String, dictionaryId: Long, groupId: Long?)
}
