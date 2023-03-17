package me.apomazkin.polytrainer.di.module.vocabulary

import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.flags.FlagProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.vocabulary.deps.VocabularyUseCase
import me.apomazkin.vocabulary.entity.LangUiEntity
import me.apomazkin.vocabulary.entity.LexemeUiItem
import me.apomazkin.vocabulary.entity.TermUiItem
import me.apomazkin.vocabulary.entity.toLexemeLabel
import javax.inject.Inject

class VocabularyUseCaseImpl @Inject constructor(
    private val dbApi: CoreDbApi,
    private val prefsProvider: PrefsProvider,
    private val flagProvider: FlagProvider,
) : VocabularyUseCase {

    override suspend fun getCurrentLang(): Int =
        prefsProvider.getInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT)

    override suspend fun getAvailableLang(): List<LangUiEntity> =
        dbApi.getLangSuspend().map {
            LangUiEntity(
                iconRes = flagProvider.getFlagRes(it.numericCode),
                title = it.name ?: "",
                numericCode = it.numericCode,
            )
        }

    override suspend fun addWord(value: String): Long =
        prefsProvider.getInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT).let { numericCode ->
            dbApi.getLangSuspend()
                .firstOrNull { it.numericCode == numericCode }?.id?.also {
                    dbApi.addWordSuspend(value, it)
                } ?: throw IllegalStateException("Language not found")
        }

    override suspend fun deleteWord(wordId: Long) {
        dbApi.deleteWordSuspend(wordId)
    }

    override suspend fun changeLang(numericCode: Int) {
        prefsProvider.setInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT, numericCode)
    }

    // TODO: Передавать langId сюда в параметр. нехер делать лишний запрос
    override suspend fun getWordList(): List<TermUiItem> {
        return prefsProvider.getInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT).let { numericCode ->
            dbApi.getLangSuspend()
                .firstOrNull { it.numericCode == numericCode }?.id?.let {
                    dbApi.getTermList(it)
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
                                lexemeList = term.defList.map { defMate ->
                                    LexemeUiItem(
                                        id = defMate.id,
                                        wordId = defMate.wordId,
                                        definition = defMate.value,
                                        category = defMate.category.toLexemeLabel(),
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

    override suspend fun deleteLexeme(lexemeId: Long) {
        dbApi.deleteLexemeSuspend(lexemeId)
    }
}