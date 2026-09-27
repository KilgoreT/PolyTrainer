package me.apomazkin.polytrainer.di.module.vocabulary

import kotlinx.coroutines.flow.Flow
import me.apomazkin.polytrainer.di.module.dictionary.CurrentDictionaryProvider
import me.apomazkin.vocabulary.deps.VocabularyHostUseCase
import javax.inject.Inject

/**
 * IS493 Э2 (D9.1): host резолвит текущий словарь для вкладок. Семантика —
 * как у words `flowCurrentDict` (один prefs-источник, рассинхрона нет, В3),
 * но узкий выход: только id (без DictUiEntity — vocabulary не тянет
 * dictionarypicker, ревью A-6/F-2). IS500: резолв — через единый
 * [CurrentDictionaryProvider].
 */
class VocabularyHostUseCaseImpl @Inject constructor(
    private val currentDictionaryProvider: CurrentDictionaryProvider,
) : VocabularyHostUseCase {

    override fun flowCurrentDictId(): Flow<Long?> =
        currentDictionaryProvider.flowCurrentDictId()
}
