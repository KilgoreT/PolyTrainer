package me.apomazkin.wordcard.mate

import kotlinx.coroutines.CancellationException
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateTypedEffectHandler
import me.apomazkin.wordcard.deps.RemoveComponentResult
import me.apomazkin.wordcard.deps.RemoveLexemeResult
import me.apomazkin.wordcard.deps.WordCardUseCase
import javax.inject.Inject

private const val TAG = "WordCardDatasource"

sealed interface DatasourceEffect : Effect {

    data class LoadWord(val wordId: Long) : DatasourceEffect
    data class RemoveWord(val wordId: Long) : DatasourceEffect
    data class UpdateWord(val wordId: Long, val value: String) : DatasourceEffect
    data class RemoveLexeme(val wordId: Long, val lexemeId: Long) : DatasourceEffect

    /**
     * Решение 2026-07-21: черновик живёт только пока карточка открыта — пустая
     * сохранённая лексема удаляется ТИХО при входе (WordLoaded) и при выходе
     * (flush-on-back). Без undo-снека и без ответных сообщений.
     */
    data class PurgeEmptyLexeme(val wordId: Long, val lexemeId: Long) : DatasourceEffect

    /** A3: три РАЗНЫЕ операции upsert значения компонента (impossible states impossible). */
    sealed interface UpsertComponentValue : DatasourceEffect {
        val wordId: Long
        val dictionaryId: Long
        val componentTypeId: ComponentTypeId
        val componentTypeRef: ComponentTypeRef
        val data: TemplateValues

        /** Создание NOT_IN_DB лексемы якорным значением. */
        data class CreateLexeme(
            override val wordId: Long,
            override val dictionaryId: Long,
            val pristineKey: Long,
            override val componentTypeId: ComponentTypeId,
            override val componentTypeRef: ComponentTypeRef,
            override val data: TemplateValues,
        ) : UpsertComponentValue

        /** Добавление нового значения к существующей лексеме. */
        data class AddValue(
            override val wordId: Long,
            override val dictionaryId: Long,
            val lexemeId: Long,
            val pristineKey: Long,
            override val componentTypeId: ComponentTypeId,
            override val componentTypeRef: ComponentTypeRef,
            override val data: TemplateValues,
        ) : UpsertComponentValue

        /** Обновление существующего значения. */
        data class UpdateValue(
            override val wordId: Long,
            override val dictionaryId: Long,
            val lexemeId: Long,
            val componentValueId: ComponentValueId,
            override val componentTypeId: ComponentTypeId,
            override val componentTypeRef: ComponentTypeRef,
            override val data: TemplateValues,
        ) : UpsertComponentValue
    }

    data class RemoveComponentValue(
        val componentValueId: ComponentValueId,
        val lexemeId: Long,
    ) : DatasourceEffect

    /** Trigger для AvailableComponentTypesFlowHandler (re-)subscribe. */
    data class LoadAvailableComponentTypes(val dictionaryId: Long) : DatasourceEffect

    data class RestoreLexemeWithComponents(
        val wordId: Long,
        val dictionaryId: Long,
        val snapshot: Lexeme,
    ) : DatasourceEffect
}

/**
 * ЭТАП 0: skeleton. РЕАЛИЗАЦИЯ — этап 5 (two-Msg burst Refresh+Inserted, error→OperationFailed,
 * CancellationException проброс). Сейчас no-op → тесты §9.5 red.
 */
class DatasourceEffectHandler @Inject constructor(
    private val wordCardUseCase: WordCardUseCase,
    private val logger: LexemeLogger,
) : MateTypedEffectHandler<Msg, DatasourceEffect>() {

    override fun filter(effect: Effect): DatasourceEffect? = effect as? DatasourceEffect

    override suspend fun onEffect(effect: DatasourceEffect, consumer: (Msg) -> Unit) {
        when (effect) {
            is DatasourceEffect.LoadWord -> {
                try {
                    val term = wordCardUseCase.getTermById(effect.wordId)
                    consumer(if (term != null) Msg.WordLoaded(term) else Msg.WordNotFound)
                } catch (c: CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    logger.log(LogLevel.ERROR, TAG, "LoadWord failed", t)
                    consumer(Msg.WordNotFound)
                }
            }

            is DatasourceEffect.RemoveWord -> guarded(consumer, R.string.word_card_error_remove_word) {
                if (wordCardUseCase.deleteWord(effect.wordId) > 0) consumer(Msg.NavigateBack)
                else consumer(Msg.OperationFailed(R.string.word_card_error_remove_word))
            }

            is DatasourceEffect.UpdateWord -> guarded(consumer, R.string.word_card_error_save_word) {
                if (wordCardUseCase.updateWord(effect.wordId, effect.value)) {
                    val term = wordCardUseCase.getTermById(effect.wordId)
                    if (term != null) consumer(Msg.RefreshWord(term))
                    else consumer(Msg.OperationFailed(R.string.word_card_error_save_word))
                } else {
                    consumer(Msg.OperationFailed(R.string.word_card_error_save_word))
                }
            }

            is DatasourceEffect.RemoveLexeme -> guarded(consumer, R.string.word_card_error_remove_lexeme) {
                when (val r = wordCardUseCase.deleteLexeme(effect.wordId, effect.lexemeId)) {
                    is RemoveLexemeResult.Removed -> consumer(Msg.LexemeRemoved(r.snapshot))
                    null -> consumer(Msg.OperationFailed(R.string.word_card_error_remove_lexeme))
                }
            }

            // Тихая чистка пустого черновика: результат не интересен (best-effort),
            // ошибки только в лог — юзер эту лексему уже не видит.
            is DatasourceEffect.PurgeEmptyLexeme -> try {
                wordCardUseCase.deleteLexeme(effect.wordId, effect.lexemeId)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                logger.log(LogLevel.ERROR, TAG, "PurgeEmptyLexeme failed", t)
            }

            is DatasourceEffect.UpsertComponentValue.CreateLexeme ->
                guarded(consumer, R.string.word_card_error_generic) {
                    val lex = wordCardUseCase.addLexemeWithComponent(
                        effect.wordId, effect.dictionaryId, effect.componentTypeRef, effect.data,
                    )
                    if (lex != null) consumer(Msg.LexemeDraftPromoted(lex, anchorPristineKey = effect.pristineKey))
                    else consumer(Msg.OperationFailed(R.string.word_card_error_generic))
                }

            is DatasourceEffect.UpsertComponentValue.AddValue ->
                guarded(consumer, R.string.word_card_error_generic) {
                    val result = wordCardUseCase.addComponentValue(effect.lexemeId, effect.componentTypeId, effect.data)
                    if (result != null) {
                        consumer(Msg.RefreshLexemeComponents(effect.lexemeId, result.lexeme.components))
                        consumer(Msg.ComponentValueInserted(effect.lexemeId, effect.pristineKey, result.newComponentValueId))
                    } else {
                        consumer(Msg.OperationFailed(R.string.word_card_error_generic))
                    }
                }

            is DatasourceEffect.UpsertComponentValue.UpdateValue ->
                guarded(consumer, R.string.word_card_error_generic) {
                    val lex = wordCardUseCase.updateComponentValue(effect.componentValueId, effect.lexemeId, effect.data)
                    if (lex != null) consumer(Msg.RefreshLexemeComponents(effect.lexemeId, lex.components))
                    else consumer(Msg.OperationFailed(R.string.word_card_error_generic))
                }

            is DatasourceEffect.RemoveComponentValue ->
                guarded(consumer, R.string.word_card_error_remove_lexeme) {
                    when (val r = wordCardUseCase.deleteComponentValue(effect.componentValueId, effect.lexemeId)) {
                        // IS486 фаза 3: лексема не удаляется — деградация в черновик
                        // (пустой список компонентов = draft-представление в UI).
                        is RemoveComponentResult.ComponentRemoved ->
                            consumer(Msg.RefreshLexemeComponents(effect.lexemeId, r.lexeme.components))
                        null -> consumer(Msg.OperationFailed(R.string.word_card_error_remove_lexeme))
                    }
                }

            is DatasourceEffect.RestoreLexemeWithComponents ->
                guarded(consumer, R.string.word_card_error_restore_lexeme) {
                    val restored = wordCardUseCase.restoreLexemeWithComponents(
                        effect.wordId, effect.dictionaryId, effect.snapshot,
                    )
                    if (restored != null) {
                        val term = wordCardUseCase.getTermById(effect.wordId)
                        if (term != null) consumer(Msg.WordLoaded(term))
                        else consumer(Msg.OperationFailed(R.string.word_card_error_restore_lexeme))
                    } else {
                        consumer(Msg.RestoreLexemeFailed(effect.snapshot))
                    }
                }

            // Обрабатывает AvailableComponentTypesFlowHandler (flow-handler), здесь — no-op.
            is DatasourceEffect.LoadAvailableComponentTypes -> Unit
        }
    }

    /** try/catch обёртка: CancellationException пробрасывается, прочее → OperationFailed(errorRes). */
    private suspend fun guarded(consumer: (Msg) -> Unit, errorRes: Int, block: suspend () -> Unit) {
        try {
            block()
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            logger.log(LogLevel.ERROR, TAG, "DB op failed", t)
            consumer(Msg.OperationFailed(errorRes))
        }
    }
}
