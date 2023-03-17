package me.apomazkin.vocabulary.logic

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler
import me.apomazkin.vocabulary.deps.VocabularyUseCase

/**
 * Effect
 */
internal sealed interface DatasourceEffect : Effect {

    /**
     * Effect to get available languages.
     */
    object LoadLangList : DatasourceEffect

    /**
     * Effect to get current language.
     */
    object LoadCurrentLang : DatasourceEffect

    /**
     * Effect to change current language.
     */
    data class ChangeLang(val numericCode: Int) : DatasourceEffect

    /**
     * Effect to load data.
     */
    object LoadTermData : DatasourceEffect

    /**
     * Effect to add new word.
     */
    data class AddWord(val value: String) : DatasourceEffect

    /**
     * Effect to delete word.
     */
    data class DeleteWord(val id: Long) : DatasourceEffect

    /**
     * Effect to save(add or update) lexeme.
     */
    data class SaveLexeme(
        val wordId: Long,
        val lexemeList: List<LexemeState>,
    ) : DatasourceEffect

    data class DeleteLexeme(val lexemeId: Long) : DatasourceEffect
}

/**
 * EffectHandler for datastore calls.
 */
internal class DatasourceEffectHandler(
    private val vocabularyUseCase: VocabularyUseCase,
) : MateEffectHandler<Msg, Effect> {

    override suspend fun runEffect(
        effect: Effect,
        consumer: (Msg) -> Unit
    ) {
        Log.d("##MATE##", "RunEffect: $effect")
        return when (val eff = effect as? DatasourceEffect) {
            is DatasourceEffect.LoadLangList -> {
                withContext(Dispatchers.IO) {
                    vocabularyUseCase.getAvailableLang().let {
                        TopBarActionMsg.AvailableLang(list = it)
                    }
                }
            }
            is DatasourceEffect.LoadCurrentLang -> {
                withContext(Dispatchers.IO) {
                    vocabularyUseCase.getCurrentLang()
                        .let { TopBarActionMsg.CurrentLang(numericCode = it) }
                }
            }
            is DatasourceEffect.ChangeLang -> {
                withContext(Dispatchers.IO) {
                    vocabularyUseCase.changeLang(numericCode = eff.numericCode)
                        .let { TopBarActionMsg.CurrentLang(numericCode = eff.numericCode) }
                }
            }
            is DatasourceEffect.LoadTermData -> {
                withContext(Dispatchers.IO) {
                    vocabularyUseCase.getWordList()
                        .let { Msg.TermDataLoaded(termList = it) }
                }
            }
            is DatasourceEffect.AddWord -> {
                withContext(Dispatchers.IO) {
                    vocabularyUseCase.addWord(eff.value).let { Msg.TermDataLoad }
                }
            }
            is DatasourceEffect.DeleteWord -> {
                vocabularyUseCase.deleteWord(eff.id).let { Msg.TermDataLoad }
            }
            is DatasourceEffect.SaveLexeme -> {
                withContext(Dispatchers.IO) {
                    // TODO: Проследить, чтобы обновлялись только те лексемы, которые реально были изменены
                    eff.lexemeList.forEach { lexeme ->
                        lexeme.lexemeId?.let { lexemeId ->
                            vocabularyUseCase.editLexeme(
                                eff.wordId,
                                lexemeId,
                                lexeme.category.stringValue,
                                lexeme.definition.editedText,
                            )
                        } ?: run {
                            vocabularyUseCase.addLexeme(
                                eff.wordId,
                                lexeme.category.stringValue,
                                lexeme.definition.editedText,
                            )
                        }
                    }.let { Msg.TermDataLoad }
                }
            }
            is DatasourceEffect.DeleteLexeme -> {
                withContext(Dispatchers.IO) {
                    vocabularyUseCase.deleteLexeme(eff.lexemeId)
                        .let { Msg.TermDataLoad }
                }
            }
            null -> Msg.Empty
        }.let(consumer)
    }
}