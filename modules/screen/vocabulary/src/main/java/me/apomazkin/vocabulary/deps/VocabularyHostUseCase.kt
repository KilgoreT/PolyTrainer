package me.apomazkin.vocabulary.deps

import kotlinx.coroutines.flow.Flow

/**
 * IS493 Э2 (D9.1): host — единственный резолвер текущего словаря для вкладок
 * (А9/Р8). Сигнатура узкая — только id: `DictUiEntity` не тащим, чтобы не
 * заводить dep vocabulary → dictionarypicker (ревью A-6/F-2).
 *
 * `null` в потоке — валидное состояние «словарей нет» (прецедент IS476).
 */
interface VocabularyHostUseCase {
    fun flowCurrentDictId(): Flow<Long?>
}
