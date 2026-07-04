package me.apomazkin.per_dictionary_components.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.PerDictionarySnapshot
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.Scope

/**
 * UseCase API для `PerDictionaryComponentsScreen` — scoped CRUD view над
 * user-defined component_types применимых к конкретному словарю
 * (global ∪ per-dict).
 *
 * Write-методы (`create` / `rename` / `previewDeletionImpact` / `softDelete`) —
 * те же сигнатуры что в `ComponentsManagerUseCase`; impl делегирует на общий
 * shared CRUD (решение Open Q #2 business_contract).
 */
interface PerDictionaryComponentsUseCase {

    /**
     * Подписка на active user-defined types применимые к словарю
     * (`(dictionary_id = :dictId OR dictionary_id IS NULL) AND system_key IS NULL
     *  AND removed_at IS NULL`) + valueCount within dict.
     */
    fun flowComponentsForDictionary(dictionaryId: Long): Flow<PerDictionarySnapshot>

    suspend fun createUserDefinedComponent(
        name: String,
        template: ComponentTemplate,
        isMultiple: Boolean,
        scope: Scope,
    ): CreateOutcome

    suspend fun renameComponent(
        typeId: ComponentTypeId,
        newName: String,
    ): RenameOutcome

    suspend fun previewDeletionImpact(typeId: ComponentTypeId): DeletionImpact?

    suspend fun softDeleteComponent(typeId: ComponentTypeId): DeleteOutcome

    /**
     * Edit existing user-defined component_type (IS481 phase 2). Делегирует на
     * `sharedCrud.editComponent(...)`. Те же business rules что в Manager.
     */
    suspend fun editComponent(
        typeId: ComponentTypeId,
        name: String,
        template: ComponentTemplate,
        isMultiple: Boolean,
    ): EditOutcome
}
