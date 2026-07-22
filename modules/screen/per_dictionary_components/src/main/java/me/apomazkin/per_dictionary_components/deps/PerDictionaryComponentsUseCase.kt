package me.apomazkin.per_dictionary_components.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.OptionOutcome
import me.apomazkin.lexeme.PerDictionarySnapshot
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.SetEnabledOutcome

/**
 * UseCase API для `PerDictionaryComponentsScreen` — scoped CRUD view над
 * component_types применимых к конкретному словарю (global ∪ per-dict).
 *
 * Write-методы (`create` / `previewDeletionImpact` / `softDelete` / `edit`) —
 * те же сигнатуры что в `ComponentsManagerUseCase`; impl делегирует на общий
 * shared CRUD (решение Open Q #2 business_contract).
 *
 * IS486 фаза 3: конструктор иерархии — снапшот включает builtin-строки
 * (рубильник enabled) и опции CHOICE; create/edit несут цель зависимости;
 * setComponentEnabled + CRUD опций.
 */
interface PerDictionaryComponentsUseCase {

    /**
     * Подписка на active types применимые к словарю
     * (`(dictionary_id = :dictId OR dictionary_id IS NULL) AND removed_at IS NULL`)
     * + valueCount within dict. Builtin включены (IS486); optionsByType — живые
     * опции CHOICE-типов.
     */
    fun flowComponentsForDictionary(dictionaryId: Long): Flow<PerDictionarySnapshot>

    suspend fun createUserDefinedComponent(
        name: String,
        template: ComponentTemplate,
        isMultiple: Boolean,
        scope: Scope,
        // IS486: цель зависимости (дефолты = цель-лексема + ядро) и стартовые
        // варианты CHOICE.
        core: Boolean = true,
        dependsOnTypeId: Long? = null,
        dependsOnOptionId: Long? = null,
        optionLabels: List<String> = emptyList(),
    ): CreateOutcome

    suspend fun previewDeletionImpact(typeId: ComponentTypeId): DeletionImpact?

    suspend fun softDeleteComponent(typeId: ComponentTypeId): DeleteOutcome

    /**
     * Edit existing user-defined component_type (IS481 phase 2). Делегирует на
     * `sharedCrud.editComponent(...)`. Те же business rules что в Manager.
     * IS486: + перепривязка цели (null-пара = цель-лексема).
     */
    suspend fun editComponent(
        typeId: ComponentTypeId,
        name: String,
        template: ComponentTemplate,
        isMultiple: Boolean,
        core: Boolean = true,
        dependsOnTypeId: Long? = null,
        dependsOnOptionId: Long? = null,
    ): EditOutcome

    /**
     * IS486: рубильник enabled (spec §6). Мягкий — без каскадов; выключение
     * последнего включённого ядра словаря → [SetEnabledOutcome.LastEnabledCore].
     */
    suspend fun setComponentEnabled(typeId: ComponentTypeId, enabled: Boolean): SetEnabledOutcome

    // ===== IS486: CRUD опций CHOICE (spec К1–К5) =====

    /** Добавить опцию (label, в конец списка). */
    suspend fun addOption(typeId: ComponentTypeId, label: String): OptionOutcome

    /** Переименовать опцию (label-override; id устойчив). */
    suspend fun renameOption(optionId: Long, label: String): OptionOutcome

    /**
     * Soft-delete опции + комбинированный каскад (значения-выборы + поддеревья
     * зависимых). Возвращает [OptionOutcome.Deleted] с фактическим impact.
     */
    suspend fun deleteOption(optionId: Long): OptionOutcome

    /** Read-only preview удаления опции (для confirm-диалога). `null` — не найдена. */
    suspend fun previewOptionDeletionImpact(optionId: Long): DeletionImpact?

    /**
     * IS486 умный сброс (решение 2026-07-21): preview перепривязки — сбрасываются
     * только значения лексем с невыполненным новым условием. Нулевой impact =
     * «безопасно». `null` — тип не найден / builtin / ошибка.
     */
    suspend fun previewRebindImpact(
        typeId: ComponentTypeId,
        core: Boolean,
        dependsOnTypeId: Long?,
        dependsOnOptionId: Long?,
    ): DeletionImpact?
}
