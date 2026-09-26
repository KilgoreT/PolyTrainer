package me.apomazkin.wordcard.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.RecoverableEffect
import io.github.kilgoret.mate.runSuspendCatching
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
import me.apomazkin.wordcard.deps.RemoveComponentResult
import me.apomazkin.wordcard.deps.RemoveLexemeResult
import me.apomazkin.wordcard.deps.WordCardUseCase
import javax.inject.Inject
import me.apomazkin.logger.LogTags as FeatureLogTags
import me.apomazkin.wordcard.LogTags as WordCardLogTags

private const val TAG = "WordCardDatasource"

/**
 * Провал каждого эффекта объявлен в его типе (RecoverableEffect):
 * handler ошибок не ловит, раннер доставляет onFail-Msg сам;
 * стектрейсы логирует ErrorLoggingObserver.
 */
sealed interface DatasourceEffect : Effect {
    data class LoadWord(
        val wordId: Long,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.WordNotFound
    }

    data class RemoveWord(
        val wordId: Long,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.OperationFailed(R.string.word_card_error_remove_word)
    }

    data class UpdateWord(
        val wordId: Long,
        val value: String,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.OperationFailed(R.string.word_card_error_save_word)
    }

    data class RemoveLexeme(
        val wordId: Long,
        val lexemeId: Long,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.OperationFailed(R.string.word_card_error_remove_lexeme)
    }

    /**
     * Решение 2026-07-21: черновик живёт только пока карточка открыта — пустая
     * сохранённая лексема удаляется ТИХО при входе (WordLoaded) и при выходе
     * (flush-on-back). Без undo-снека и без ответных сообщений; ошибки
     * best-effort (только лог), поэтому НЕ RecoverableEffect.
     */
    data class PurgeEmptyLexeme(
        val wordId: Long,
        val lexemeId: Long,
    ) : DatasourceEffect

    /** A3: три РАЗНЫЕ операции upsert значения компонента (impossible states impossible). */
    sealed interface UpsertComponentValue :
        DatasourceEffect,
        RecoverableEffect<Msg> {
        val wordId: Long
        val dictionaryId: Long
        val componentTypeId: ComponentTypeId
        val componentTypeRef: ComponentTypeRef
        val data: TemplateValues

        override fun onFail(error: Throwable): Msg = Msg.OperationFailed(R.string.word_card_error_generic)

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
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.OperationFailed(R.string.word_card_error_remove_lexeme)
    }

    /**
     * IS491: one-shot загрузка подсказок caption для captioned-компонента.
     * Ошибка деградирует в пустой список подсказок.
     */
    data class LoadCaptionSuggestions(
        val typeId: ComponentTypeId,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.CaptionSuggestionsLoaded(typeId, emptyList())
    }

    // Живые списки (типы компонентов, группы слова/словаря) эффектами
    // не выражаются: это подписки [WordCardSub], декларируемые из
    // state после загрузки слова.

    data class RestoreLexemeWithComponents(
        val wordId: Long,
        val dictionaryId: Long,
        val snapshot: Lexeme,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.OperationFailed(R.string.word_card_error_restore_lexeme)
    }

    // === IS493: группы слова ===

    /** Membership-мутации (запись сразу по галочке, В3). */
    data class AddMembership(
        val wordId: Long,
        val groupId: Long,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.MembershipFailed(groupId)
    }

    data class RemoveMembership(
        val wordId: Long,
        val groupId: Long,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.MembershipFailed(groupId)
    }
}

/**
 * Исполнитель эффектов карточки слова: один эффект — один вызов use
 * case, итог возвращается новым Msg (AddValue — two-Msg burst:
 * Refresh + Inserted). Ошибок не ловит — см. KDoc [DatasourceEffect].
 */
class DatasourceEffectHandler
    @Inject
    constructor(
        private val wordCardUseCase: WordCardUseCase,
        private val logger: LexemeLogger,
    ) : MateEffectHandler<Msg, DatasourceEffect> {
        override val effectFamily = DatasourceEffect::class

        override suspend fun runEffect(
            effect: DatasourceEffect,
            consumer: (Msg) -> Unit,
        ) {
            when (effect) {
                is DatasourceEffect.LoadWord -> {
                    val term = wordCardUseCase.getTermById(effect.wordId)
                    consumer(if (term != null) Msg.WordLoaded(term) else Msg.WordNotFound)
                }

                is DatasourceEffect.RemoveWord ->
                    if (wordCardUseCase.deleteWord(effect.wordId) > 0) {
                        consumer(Msg.NavigateBack)
                    } else {
                        consumer(Msg.OperationFailed(R.string.word_card_error_remove_word))
                    }

                is DatasourceEffect.UpdateWord ->
                    if (wordCardUseCase.updateWord(effect.wordId, effect.value)) {
                        val term = wordCardUseCase.getTermById(effect.wordId)
                        if (term != null) {
                            consumer(Msg.RefreshWord(term))
                        } else {
                            consumer(Msg.OperationFailed(R.string.word_card_error_save_word))
                        }
                    } else {
                        consumer(Msg.OperationFailed(R.string.word_card_error_save_word))
                    }

                is DatasourceEffect.RemoveLexeme ->
                    when (val r = wordCardUseCase.deleteLexeme(effect.wordId, effect.lexemeId)) {
                        is RemoveLexemeResult.Removed -> consumer(Msg.LexemeRemoved(r.snapshot))
                        null -> consumer(Msg.OperationFailed(R.string.word_card_error_remove_lexeme))
                    }

                // Тихая чистка пустого черновика: результат не интересен (best-effort),
                // ошибки только в лог — юзер эту лексему уже не видит.
                is DatasourceEffect.PurgeEmptyLexeme ->
                    runSuspendCatching { wordCardUseCase.deleteLexeme(effect.wordId, effect.lexemeId) }
                        .onFailure { logger.log(LogLevel.ERROR, TAG, "PurgeEmptyLexeme failed", it) }

                is DatasourceEffect.UpsertComponentValue.CreateLexeme -> {
                    val lex = wordCardUseCase.addLexemeWithComponent(
                        effect.wordId,
                        effect.dictionaryId,
                        effect.componentTypeRef,
                        effect.data,
                    )
                    if (lex != null) {
                        consumer(Msg.LexemeDraftPromoted(lex, anchorPristineKey = effect.pristineKey))
                    } else {
                        consumer(Msg.OperationFailed(R.string.word_card_error_generic))
                    }
                }

                is DatasourceEffect.UpsertComponentValue.AddValue -> {
                    val result = wordCardUseCase.addComponentValue(effect.lexemeId, effect.componentTypeId, effect.data)
                    if (result != null) {
                        logCaptionedUpsert("value add", effect.data, effect.componentTypeId, result.newComponentValueId)
                        consumer(Msg.RefreshLexemeComponents(effect.lexemeId, result.lexeme.components))
                        consumer(Msg.ComponentValueInserted(effect.lexemeId, effect.pristineKey, result.newComponentValueId))
                    } else {
                        consumer(Msg.OperationFailed(R.string.word_card_error_generic))
                    }
                }

                is DatasourceEffect.UpsertComponentValue.UpdateValue -> {
                    val lex = wordCardUseCase.updateComponentValue(effect.componentValueId, effect.lexemeId, effect.data)
                    if (lex != null) {
                        logCaptionedUpsert("value update", effect.data, typeId = null, effect.componentValueId)
                        consumer(Msg.RefreshLexemeComponents(effect.lexemeId, lex.components))
                    } else {
                        consumer(Msg.OperationFailed(R.string.word_card_error_generic))
                    }
                }

                is DatasourceEffect.RemoveComponentValue ->
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

                is DatasourceEffect.LoadCaptionSuggestions ->
                    consumer(
                        Msg.CaptionSuggestionsLoaded(
                            effect.typeId,
                            wordCardUseCase.getCaptionSuggestions(effect.typeId),
                        ),
                    )

                is DatasourceEffect.RestoreLexemeWithComponents -> {
                    val restored = wordCardUseCase.restoreLexemeWithComponents(
                        effect.wordId,
                        effect.dictionaryId,
                        effect.snapshot,
                    )
                    if (restored != null) {
                        val term = wordCardUseCase.getTermById(effect.wordId)
                        if (term != null) {
                            consumer(Msg.WordLoaded(term))
                        } else {
                            consumer(Msg.OperationFailed(R.string.word_card_error_restore_lexeme))
                        }
                    } else {
                        consumer(Msg.RestoreLexemeFailed(effect.snapshot))
                    }
                }

                // === IS493: membership-мутации (плоские Msg через маппер) ===

                is DatasourceEffect.AddMembership -> {
                    val outcome = wordCardUseCase.addWordToGroup(effect.wordId, effect.groupId)
                    logger.d(
                        tag = WordCardLogTags.WORDCARD,
                        message = "membership add: word=${effect.wordId} group=${effect.groupId} outcome=$outcome",
                    )
                    consumer(outcome.toMembershipMsg(effect.groupId))
                }

                is DatasourceEffect.RemoveMembership -> {
                    val outcome = wordCardUseCase.removeWordFromGroup(effect.wordId, effect.groupId)
                    logger.d(
                        tag = WordCardLogTags.WORDCARD,
                        message = "membership remove: word=${effect.wordId} group=${effect.groupId} outcome=$outcome",
                    )
                    consumer(outcome.toMembershipMsg(effect.groupId))
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
    }
