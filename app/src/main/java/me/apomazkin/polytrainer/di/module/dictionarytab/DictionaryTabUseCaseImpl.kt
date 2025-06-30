package me.apomazkin.polytrainer.di.module.dictionarytab

import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarytab.deps.DictionaryTabUseCase
import me.apomazkin.dictionarytab.deps.LangNotFoundException
import me.apomazkin.dictionarytab.entity.DefinitionUiEntity
import me.apomazkin.dictionarytab.entity.LexemeUiItem
import me.apomazkin.dictionarytab.entity.TermUiItem
import me.apomazkin.dictionarytab.entity.TranslationUiEntity
import me.apomazkin.flags.FlagProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Inject

class DictionaryTabUseCaseImpl @Inject constructor(
        private val dbApi: CoreDbApi,
        private val langApi: CoreDbApi.LangApi,
        private val termApi: CoreDbApi.TermApi,
        private val lexemeApi: CoreDbApi.LexemeApi,
        private val prefsProvider: PrefsProvider,
        private val flagProvider: FlagProvider,
) : DictionaryTabUseCase {

    override suspend fun getLangId(numericCode: Int): Int {
        return langApi.getLang(numericCode)?.id ?: 0
    }

    override suspend fun getCurrentDict(): DictUiEntity {
        prefsProvider
                .getInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT)?.let { num ->
                    langApi.getLang(numericCode = num)?.let {
                        return DictUiEntity(
                                flagRes = flagProvider.getFlagRes(it.numericCode),
                                title = it.name,
                                numericCode = it.numericCode,
                        )
                    }
                } ?: langApi.getLangList()
                .firstOrNull()
                ?.let {
                    prefsProvider.setInt(
                            PrefKey.CURRENT_LANG_NUMERIC_CODE_INT,
                            it.numericCode
                    )
                    return DictUiEntity(
                            flagRes = flagProvider.getFlagRes(it.numericCode),
                            title = it.name,
                            numericCode = it.numericCode,
                    )
                }
        throw LangNotFoundException()
    }

    override fun flowCurrentDict(): Flow<DictUiEntity> = prefsProvider
            .getIntFlow(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT)
            .map { numeric: Int ->
                val lang = (langApi
                        .getLang(numeric)
                        ?: langApi.getLangList().firstOrNull())
                        ?.let { lang ->
                            DictUiEntity(
                                    flagRes = flagProvider.getFlagRes(lang.numericCode),
                                    title = lang.name,
                                    numericCode = lang.numericCode,
                            )
                        }
                lang ?: throw LangNotFoundException()
            }

    // TODO: Убрать нулеабельность в Language: id и name
    override suspend fun addWord(value: String): Long =
            prefsProvider
                    .getInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT)
                    ?.let { numericCode ->
                        langApi.getLang(numericCode)?.let {
                            return dbApi.addWordSuspend(value, it.id)
                        } ?: throw IllegalStateException("Language not found")
                    } ?: throw IllegalStateException("Language not found")

    override suspend fun updateWord(id: Long, value: String): Boolean =
            dbApi.updateWordSuspend(id, value)

    override suspend fun deleteWord(wordId: Long) {
        dbApi.deleteWordSuspend(wordId)
    }

    override suspend fun changeDict(numericCode: Int) {
        prefsProvider.setInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT, numericCode)
    }

    // TODO: Передавать langId сюда в параметр. нехер делать лишний запрос
    // langid хранить в стейте
    override suspend fun getWordList(): List<TermUiItem> {
        return prefsProvider
                .getInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT)
                ?.let { numericCode ->
                    langApi.getLang(numericCode = numericCode)?.id?.let { langId: Int ->
                        termApi.getTermList(langId)
                                .map { term ->
                                    TermUiItem(
                                            id = term.word.id,
                                            wordValue = term.word.value,
                                            langId = term.word.langId,
                                            addDate = term.word.addDate,
                                            changeDate = term.word.changeDate,
                                            lexemeList = term.lexemes.map { defMate ->
                                                LexemeUiItem(
                                                        id = defMate.id,
                                                        wordId = defMate.wordId,
                                                        translation = defMate.translation?.let {
                                                            TranslationUiEntity(
                                                                    it.value
                                                            )
                                                        },
                                                        definition = defMate.definition?.let { DefinitionUiEntity(it.value) },
                                                        addDate = defMate.addDate,
                                                        changeDate = defMate.changeDate,
                                                )
                                            }
                                    )
                                }
                    } ?: throw IllegalStateException("Language not found")
                } ?: throw IllegalStateException("Language not found")
    }

    override fun searchTerms(pattern: String, langId: Int): Flow<PagingData<TermUiItem>> {
        return termApi.searchTermsPaging(
                pattern = pattern,
                langId = langId
        ).map { pagingData ->
            pagingData.map { term ->
                TermUiItem(
                        id = term.word.id,
                        wordValue = term.word.value,
                        langId = term.word.langId,
                        addDate = term.word.addDate,
                        changeDate = term.word.changeDate,
                        lexemeList = term.lexemes.map { defMate ->
                            LexemeUiItem(
                                    id = defMate.id,
                                    wordId = defMate.wordId,
                                    translation = defMate.translation?.let {
                                        TranslationUiEntity(
                                                it.value
                                        )
                                    },
                                    definition = defMate.definition?.let { DefinitionUiEntity(it.value) },
                                    addDate = defMate.addDate,
                                    changeDate = defMate.changeDate,
                            )
                        }
                )
            }

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
            definition: String,
    ) {
        dbApi.editLexemeSuspend(wordId, lexemeId, category, definition)
    }

    override suspend fun deleteLexeme(lexemeId: Long): Boolean {
        return lexemeApi.deleteLexeme(id = lexemeId) > 0
    }
}