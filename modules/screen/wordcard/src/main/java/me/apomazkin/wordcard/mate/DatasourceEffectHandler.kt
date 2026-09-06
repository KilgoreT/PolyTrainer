package me.apomazkin.wordcard.mate

import kotlinx.coroutines.CancellationException
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.CaptionedTextValues
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.logger.LogTags as FeatureLogTags
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler
import me.apomazkin.wordcard.LogTags as WordCardLogTags
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
        /** IS491: шаблон удаляемого значения — для фичевого лог-контракта. */
        val template: ComponentTemplate = ComponentTemplate.TEXT,
    ) : DatasourceEffect

    /** IS491: one-shot загрузка подсказок caption для captioned-компонента. */
    data class LoadCaptionSuggestions(val typeId: ComponentTypeId) : DatasourceEffect

    /** Trigger для AvailableComponentTypesFlowHandler (re-)subscribe. */
    data class LoadAvailableComponentTypes(val dictionaryId: Long) : DatasourceEffect

    data class RestoreLexemeWithComponents(
        val wordId: Long,
        val dictionaryId: Long,
        val snapshot: Lexeme,
    ) : DatasourceEffect

    // === IS493 Э5 (D22): группы слова ===

    /** Trigger для GroupBlockFlowHandler: подписки wordGroups+dictGroups
     * (единственная строка в ветке WordLoaded — ревью Mate-2). */
    data class SubscribeGroupBlock(val wordId: Long, val dictionaryId: Long) : DatasourceEffect

    /** Membership-мутации (запись сразу по галочке, В3). */
    data class AddMembership(val wordId: Long, val groupId: Long) : DatasourceEffect
    data class RemoveMembership(val wordId: Long, val groupId: Long) : DatasourceEffect
}

/**
 * ЭТАП 0: skeleton. РЕАЛИЗАЦИЯ — этап 5 (two-Msg burst Refresh+Inserted, error→OperationFailed,
 * CancellationException проброс). Сейчас no-op → тесты §9.5 red.
 */
class DatasourceEffectHandler @Inject constructor(
    private val wordCardUseCase: WordCardUseCase,
    val availableComponentTypesFlowHandler: AvailableComponentTypesFlowHandler,
    val groupBlockFlowHandler: GroupBlockFlowHandler,
    private val logger: LexemeLogger,
) : MateEffectHandler<Msg, DatasourceEffect> {

    override val effectFamily = DatasourceEffect::class

    override suspend fun runEffect(effect: DatasourceEffect, consumer: (Msg) -> Unit) {
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
                        logCaptionedUpsert("value add", effect.data, effect.componentTypeId, result.newComponentValueId)
                        consumer(Msg.RefreshLexemeComponents(effect.lexemeId, result.lexeme.components))
                        consumer(Msg.ComponentValueInserted(effect.lexemeId, effect.pristineKey, result.newComponentValueId))
                    } else {
                        consumer(Msg.OperationFailed(R.string.word_card_error_generic))
                    }
                }

            is DatasourceEffect.UpsertComponentValue.UpdateValue ->
                guarded(consumer, R.string.word_card_error_generic) {
                    val lex = wordCardUseCase.updateComponentValue(effect.componentValueId, effect.lexemeId, effect.data)
                    if (lex != null) {
                        logCaptionedUpsert("value update", effect.data, typeId = null, effect.componentValueId)
                        consumer(Msg.RefreshLexemeComponents(effect.lexemeId, lex.components))
                    } else {
                        consumer(Msg.OperationFailed(R.string.word_card_error_generic))
                    }
                }

            is DatasourceEffect.RemoveComponentValue ->
                guarded(consumer, R.string.word_card_error_remove_lexeme) {
                    when (val r = wordCardUseCase.deleteComponentValue(effect.componentValueId, effect.lexemeId)) {
                        // IS486 фаза 3: лексема не удаляется — деградация в черновик
                        // (пустой список компонентов = draft-представление в UI).
                        is RemoveComponentResult.ComponentRemoved -> {
                            if (effect.template == ComponentTemplate.CAPTIONED_TEXT) {
                                logger.d(
                                    tag = FeatureLogTags.CAPTIONED_TEXT,
                                    message = "value remove: valueId=${effect.componentValueId.id}",
                                )
                            }
                            consumer(Msg.RefreshLexemeComponents(effect.lexemeId, r.lexeme.components))
                        }
                        null -> consumer(Msg.OperationFailed(R.string.word_card_error_remove_lexeme))
                    }
                }

            // IS491: подсказки — best-effort: ошибка → пустой список + лог.
            is DatasourceEffect.LoadCaptionSuggestions -> try {
                consumer(
                    Msg.CaptionSuggestionsLoaded(
                        effect.typeId,
                        wordCardUseCase.getCaptionSuggestions(effect.typeId),
                    ),
                )
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                logger.log(LogLevel.ERROR, TAG, "LoadCaptionSuggestions failed", t)
                consumer(Msg.CaptionSuggestionsLoaded(effect.typeId, emptyList()))
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

            // (Re-)subscribe делегируется AvailableComponentTypesFlowHandler.
            is DatasourceEffect.LoadAvailableComponentTypes ->
                availableComponentTypesFlowHandler.resubscribe(effect)

            // (Re-)subscribe делегируется GroupBlockFlowHandler.
            is DatasourceEffect.SubscribeGroupBlock ->
                groupBlockFlowHandler.resubscribe(effect)

            // === IS493 Э5: membership-мутации (плоские Msg через маппер) ===

            is DatasourceEffect.AddMembership -> {
                try {
                    val outcome = wordCardUseCase.addWordToGroup(effect.wordId, effect.groupId)
                    logger.d(
                        tag = WordCardLogTags.WORDCARD,
                        message = "membership add: word=${effect.wordId} group=${effect.groupId} outcome=$outcome",
                    )
                    consumer(outcome.toMembershipMsg(effect.groupId))
                } catch (c: CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    logger.log(
                        LogLevel.ERROR, WordCardLogTags.WORDCARD,
                        "membership add failed: word=${effect.wordId} group=${effect.groupId}", t,
                    )
                    consumer(Msg.MembershipFailed(effect.groupId))
                }
            }

            is DatasourceEffect.RemoveMembership -> {
                try {
                    val outcome = wordCardUseCase.removeWordFromGroup(effect.wordId, effect.groupId)
                    logger.d(
                        tag = WordCardLogTags.WORDCARD,
                        message = "membership remove: word=${effect.wordId} group=${effect.groupId} outcome=$outcome",
                    )
                    consumer(outcome.toMembershipMsg(effect.groupId))
                } catch (c: CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    logger.log(
                        LogLevel.ERROR, WordCardLogTags.WORDCARD,
                        "membership remove failed: word=${effect.wordId} group=${effect.groupId}", t,
                    )
                    consumer(Msg.MembershipFailed(effect.groupId))
                }
            }
        }
    }

    /** IS491 лог-контракт: value add/update — только для captioned-значений. */
    private fun logCaptionedUpsert(
        event: String,
        data: TemplateValues,
        typeId: ComponentTypeId?,
        valueId: ComponentValueId,
    ) {
        val captioned = data as? CaptionedTextValues ?: return
        val typePart = typeId?.let { "typeId=${it.id} " } ?: ""
        logger.d(
            tag = FeatureLogTags.CAPTIONED_TEXT,
            message = "$event: ${typePart}valueId=${valueId.id} " +
                "text.len=${captioned.text.value.length} caption='${captioned.caption?.value ?: "null"}'",
        )
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
