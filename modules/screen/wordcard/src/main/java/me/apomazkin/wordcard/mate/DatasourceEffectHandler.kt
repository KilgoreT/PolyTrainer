package me.apomazkin.wordcard.mate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.core_resources.R
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.Effect
import me.apomazkin.mate.LogTags
import me.apomazkin.mate.MateTypedEffectHandler
import me.apomazkin.wordcard.deps.RemoveDefinitionResult
import me.apomazkin.wordcard.deps.RemoveTranslationResult
import me.apomazkin.wordcard.deps.WordCardUseCase
import javax.inject.Inject

sealed interface DatasourceEffect : Effect {

    data class LoadWord(val wordId: Long) : DatasourceEffect
    data class RemoveWord(val wordId: Long) : DatasourceEffect
    data class UpdateWord(val wordId: Long, val value: String) : DatasourceEffect

    data class RemoveLexeme(val wordId: Long, val lexemeId: Long) : DatasourceEffect

    data class UpdateLexemeTranslation(
        val wordId: Long,
        /** null ⇒ insert новой лексемы с переводом. */
        val lexemeId: Long?,
        val translation: String,
    ) : DatasourceEffect

    data class RemoveTranslation(val lexemeId: Long, val currentValue: String) : DatasourceEffect

    data class UpdateLexemeDefinition(
        val wordId: Long,
        /** null ⇒ insert новой лексемы с определением. */
        val lexemeId: Long?,
        val definition: String,
    ) : DatasourceEffect

    data class RemoveDefinition(val lexemeId: Long, val currentValue: String) : DatasourceEffect

    data class RestoreLexeme(
        val wordId: Long,
        val translation: String?,
        val definition: String?,
    ) : DatasourceEffect
}

class DatasourceEffectHandler @Inject constructor(
    private val wordCardUseCase: WordCardUseCase,
    private val logger: LexemeLogger,
) : MateTypedEffectHandler<Msg, DatasourceEffect>() {

    override fun filter(effect: Effect): DatasourceEffect? = effect as? DatasourceEffect

    override suspend fun onEffect(effect: DatasourceEffect, consumer: (Msg) -> Unit) {
        val msg: Msg = withContext(Dispatchers.IO) {
            try {
                when (effect) {
                    is DatasourceEffect.LoadWord ->
                        wordCardUseCase.getTermById(effect.wordId)
                            ?.let { Msg.WordLoaded(it) }
                            ?: Msg.WordNotFound

                    is DatasourceEffect.RemoveWord -> {
                        val deleted = wordCardUseCase.deleteWord(effect.wordId)
                        if (deleted > 0) Msg.NavigateBack
                        else Msg.ShowError(R.string.word_card_error_remove_word)
                    }

                    is DatasourceEffect.UpdateWord -> {
                        val ok = wordCardUseCase.updateWord(effect.wordId, effect.value)
                        if (!ok) {
                            Msg.ShowError(R.string.word_card_error_save_word)
                        } else {
                            wordCardUseCase.getTermById(effect.wordId)
                                ?.let { Msg.RefreshWord(it) }
                                ?: Msg.ShowError(R.string.word_card_error_refresh_word)
                        }
                    }

                    is DatasourceEffect.RemoveLexeme -> {
                        // Snapshot subentities ДО delete — нужно для undo.
                        val snapshot = wordCardUseCase.getTermById(effect.wordId)
                            ?.lexemeList?.firstOrNull { it.lexemeId.id == effect.lexemeId }
                        val translationSnapshot = snapshot?.translation?.value
                        val definitionSnapshot = snapshot?.definition?.value
                        wordCardUseCase.deleteLexeme(effect.wordId, effect.lexemeId)
                            ?.let { _ ->
                                Msg.LexemeRemoved(
                                    lexemeId = effect.lexemeId,
                                    translation = translationSnapshot,
                                    definition = definitionSnapshot,
                                )
                            }
                            ?: Msg.ShowError(R.string.word_card_error_remove_lexeme)
                    }

                    is DatasourceEffect.UpdateLexemeTranslation ->
                        wordCardUseCase.addLexemeTranslation(
                            wordId = effect.wordId,
                            lexemeId = effect.lexemeId,
                            translation = effect.translation,
                        )?.let { lex ->
                            Msg.RefreshTranslation(
                                lexemeId = lex.lexemeId.id,
                                translation = lex.translation?.value,
                            )
                        } ?: Msg.ShowError(R.string.word_card_error_save_translation)

                    is DatasourceEffect.RemoveTranslation ->
                        when (val r = wordCardUseCase.deleteLexemeTranslation(effect.lexemeId)) {
                            is RemoveTranslationResult.TranslationRemoved ->
                                Msg.TranslationDeleted(
                                    lexemeId = r.lexeme.lexemeId.id,
                                    removedValue = effect.currentValue,
                                )
                            RemoveTranslationResult.LexemeCascadeRemoved ->
                                Msg.LexemeCascadeRemovedWithUndo(
                                    lexemeId = effect.lexemeId,
                                    removedTranslation = effect.currentValue,
                                    removedDefinition = null,
                                )
                            null ->
                                Msg.ShowError(R.string.word_card_error_remove_translation)
                        }

                    is DatasourceEffect.UpdateLexemeDefinition ->
                        wordCardUseCase.addLexemeDefinition(
                            wordId = effect.wordId,
                            lexemeId = effect.lexemeId,
                            definition = effect.definition,
                        )?.let { lex ->
                            Msg.RefreshDefinition(
                                lexemeId = lex.lexemeId.id,
                                definition = lex.definition?.value,
                            )
                        } ?: Msg.ShowError(R.string.word_card_error_save_definition)

                    is DatasourceEffect.RemoveDefinition ->
                        when (val r = wordCardUseCase.deleteLexemeDefinition(effect.lexemeId)) {
                            is RemoveDefinitionResult.DefinitionRemoved ->
                                Msg.DefinitionDeleted(
                                    lexemeId = r.lexeme.lexemeId.id,
                                    removedValue = effect.currentValue,
                                )
                            RemoveDefinitionResult.LexemeCascadeRemoved ->
                                Msg.LexemeCascadeRemovedWithUndo(
                                    lexemeId = effect.lexemeId,
                                    removedTranslation = null,
                                    removedDefinition = effect.currentValue,
                                )
                            null ->
                                Msg.ShowError(R.string.word_card_error_remove_definition)
                        }

                    is DatasourceEffect.RestoreLexeme ->
                        wordCardUseCase.restoreLexeme(
                            wordId = effect.wordId,
                            translation = effect.translation,
                            definition = effect.definition,
                        )?.let { Msg.RefreshLexemeList(it) }
                            ?: Msg.ShowError(R.string.word_card_error_restore_lexeme)
                }
            } catch (e: Exception) {
                logger.e(
                    tag = LogTags.MATE,
                    message = "DatasourceEffect failed: ${effect::class.simpleName} — ${e.message}",
                )
                when (effect) {
                    is DatasourceEffect.LoadWord -> Msg.WordNotFound
                    is DatasourceEffect.RemoveWord -> Msg.ShowError(R.string.word_card_error_remove_word)
                    is DatasourceEffect.UpdateWord -> Msg.ShowError(R.string.word_card_error_save_word)
                    is DatasourceEffect.RemoveLexeme -> Msg.ShowError(R.string.word_card_error_remove_lexeme)
                    is DatasourceEffect.UpdateLexemeTranslation -> Msg.ShowError(R.string.word_card_error_save_translation)
                    is DatasourceEffect.RemoveTranslation -> Msg.ShowError(R.string.word_card_error_remove_translation)
                    is DatasourceEffect.UpdateLexemeDefinition -> Msg.ShowError(R.string.word_card_error_save_definition)
                    is DatasourceEffect.RemoveDefinition -> Msg.ShowError(R.string.word_card_error_remove_definition)
                    is DatasourceEffect.RestoreLexeme -> Msg.ShowError(R.string.word_card_error_restore_lexeme)
                }
            }
        }
        consumer(msg)
    }
}
