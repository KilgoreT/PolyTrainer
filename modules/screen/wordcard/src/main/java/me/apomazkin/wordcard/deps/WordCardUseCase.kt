package me.apomazkin.wordcard.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.lexeme.ComponentOption
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.wordcard.entity.Term

/**
 * IS481 generic UseCase (AGG-12). Перевод/определение — обычные компоненты,
 * без translation/definition-специфичных методов.
 */
interface WordCardUseCase {

    // ===== Word / lexeme =====
    suspend fun getTermById(wordId: Long): Term?
    suspend fun deleteWord(wordId: Long): Int
    suspend fun updateWord(wordId: Long, value: String): Boolean
    suspend fun deleteLexeme(wordId: Long, lexemeId: Long): RemoveLexemeResult?

    // ===== Generic component API =====

    /** Создать лексему первым (якорным) значением. null — тип не найден в словаре. */
    suspend fun addLexemeWithComponent(
        wordId: Long,
        dictionaryId: Long,
        ref: ComponentTypeRef,
        data: TemplateValues,
    ): Lexeme?

    /** Добавить значение к существующей лексеме. */
    suspend fun addComponentValue(
        lexemeId: Long,
        componentTypeId: ComponentTypeId,
        data: TemplateValues,
    ): AddComponentValueResult?

    /** Обновить значение; `lexemeId` — для re-read (A1), не для записи. */
    suspend fun updateComponentValue(
        componentValueId: ComponentValueId,
        lexemeId: Long,
        data: TemplateValues,
    ): Lexeme?

    /**
     * Удалить значение. IS486 фаза 3 (spec §9.1): лексема НЕ удаляется —
     * потеря последнего значения деградирует её в черновик (пустая карточка с чипами).
     */
    suspend fun deleteComponentValue(
        componentValueId: ComponentValueId,
        lexemeId: Long,
    ): RemoveComponentResult?

    /** Восстановить лексему из snapshot (undo) — atomic compound INSERT. */
    suspend fun restoreLexemeWithComponents(
        wordId: Long,
        dictionaryId: Long,
        snapshot: Lexeme,
    ): Lexeme?

    /**
     * Реактивный поток active component types словаря (built-in + user-defined).
     * IS486: вместе с опциями CHOICE-типов ([AvailableComponents.optionsByType]).
     */
    fun flowAvailableComponentTypes(dictionaryId: Long): Flow<AvailableComponents>
}

/**
 * IS486: снапшот доступных компонентов словаря для карточки.
 * [types] — активные типы; [optionsByType] — живые опции CHOICE-типов.
 */
data class AvailableComponents(
    val types: List<ComponentType>,
    val optionsByType: Map<ComponentTypeId, List<ComponentOption>> = emptyMap(),
)

/** Квитанция addComponentValue — детерминированная пара pristine → newCvId. */
data class AddComponentValueResult(
    val lexeme: Lexeme,
    val newComponentValueId: ComponentValueId,
)

sealed interface RemoveComponentResult {
    data class ComponentRemoved(val lexeme: Lexeme) : RemoveComponentResult
    // IS486 фаза 3: LexemeCascadeRemoved упразднён — лексема деградирует
    // в черновик (spec §9.1, решение В4), а не удаляется.
}

sealed interface RemoveLexemeResult {
    data class Removed(val snapshot: Lexeme) : RemoveLexemeResult
}
