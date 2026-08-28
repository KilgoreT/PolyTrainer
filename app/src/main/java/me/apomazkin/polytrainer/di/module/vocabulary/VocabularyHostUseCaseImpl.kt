package me.apomazkin.polytrainer.di.module.vocabulary

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.vocabulary.deps.VocabularyHostUseCase
import javax.inject.Inject

/**
 * IS493 Э2 (D9.1): host резолвит текущий словарь для вкладок. Семантика —
 * как у words `flowCurrentDict` (один prefs-источник, рассинхрона нет, В3),
 * но узкий выход: только id (без DictUiEntity — vocabulary не тянет
 * dictionarypicker, ревью A-6/F-2).
 */
class VocabularyHostUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val prefsProvider: PrefsProvider,
) : VocabularyHostUseCase {

    override fun flowCurrentDictId(): Flow<Long?> = prefsProvider
        .getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
        .map { id: Long? ->
            // null — валидное «словарей нет» (IS476); fallback на первый
            // словарь — как у words (id в prefs может отсутствовать/протухнуть).
            (id?.let { dictionaryApi.getDictionaryById(it) }
                ?: dictionaryApi.getDictionaryList().firstOrNull())
                ?.id
        }
}
