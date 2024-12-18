package me.apomazkin.polytrainer.di.module.vocabulary

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.flags.FlagProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.vocabulary.deps.VocabularyUseCase
import me.apomazkin.vocabulary.entity.DictUiEntity
import me.apomazkin.vocabulary.entity.LexemeUiItem
import me.apomazkin.vocabulary.entity.TermUiItem
import me.apomazkin.vocabulary.entity.toLexemeLabel
import javax.inject.Inject

class VocabularyUseCaseImpl @Inject constructor(
    private val dbApi: CoreDbApi,
    private val termApi: CoreDbApi.TermApi,
    private val lexemeApi: CoreDbApi.LexemeApi,
    private val prefsProvider: PrefsProvider,
    private val flagProvider: FlagProvider,
) : VocabularyUseCase {

    override suspend fun getCurrentDict(): Int =
        prefsProvider.getInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT)

    override suspend fun getAvailableDict(): List<DictUiEntity> =
        dbApi.getLangSuspend().map {
            DictUiEntity(
                flagRes = flagProvider.getFlagRes(it.numericCode),
                title = it.name ?: "",
                numericCode = it.numericCode,
            )
        }

    override fun flowAvailableDict(): Flow<List<DictUiEntity>> =
        dbApi.flowLang().map {
            it.map { lang ->
                DictUiEntity(
                    flagRes = flagProvider.getFlagRes(lang.numericCode),
                    title = lang.name ?: "",
                    numericCode = lang.numericCode,
                )
            }
        }

    override suspend fun addWord(value: String): Long =
        prefsProvider.getInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT).let { numericCode ->
            dbApi.getLangSuspend()
                .firstOrNull { it.numericCode == numericCode }?.id?.also {
                    dbApi.addWordSuspend(value, it)
                } ?: throw IllegalStateException("Language not found")
        }

    override suspend fun updateWord(id: Long, value: String): Boolean =
        dbApi.updateWordSuspend(id, value)

    override suspend fun deleteWord(wordId: Long) {
        dbApi.deleteWordSuspend(wordId)
    }

    override suspend fun changeDict(numericCode: Int) {
        prefsProvider.setInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT, numericCode)
    }

    // TODO: Передавать langId сюда в параметр. нехер делать лишний запрос
    override suspend fun getWordList(): List<TermUiItem> {
        return prefsProvider.getInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT).let { numericCode ->
            dbApi.getLangSuspend()
                .firstOrNull { it.numericCode == numericCode }?.id?.let {
                    termApi.getTermList(it)
                        .map { term ->
                            TermUiItem(
                                id = term.word.id
                                    ?: throw IllegalStateException("Word id not found"),
                                wordValue = term.word.value
                                    ?: throw IllegalStateException("Word value not found"),
                                langId = term.word.langId,
                                addDate = term.word.addDate
                                    ?: throw IllegalStateException("Word addData not found"),
                                changeDate = term.word.changeDate,
                                lexemeList = term.lexemes.map { defMate ->
                                    LexemeUiItem(
                                        id = defMate.id,
                                        wordId = defMate.wordId
                                            ?: throw IllegalStateException("Word id not found"),
                                        definition = "defMate.value",
//                                        category = defMate.category.toLexemeLabel(),
                                        category = "".toLexemeLabel(),
                                    )
                                }
                            )
                        }
                } ?: throw IllegalStateException("Language not found")
        }
    }

    override suspend fun addLexeme(
        wordId: Long,
        category: String,
        definition: String,
    ) {
        dbApi.addLexemeSuspend(wordId, category, definition)
    }

    override suspend fun editLexeme(
        wordId: Long,
        lexemeId: Long,
        category: String,
        definition: String
    ) {
        dbApi.editLexemeSuspend(wordId, lexemeId, category, definition)
    }

    override suspend fun deleteLexeme(lexemeId: Long): Boolean {
        return lexemeApi.deleteLexeme(id = lexemeId) > 0
    }
}