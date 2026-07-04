package me.apomazkin.polytrainer.di.module.perdictionarycomponents

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.PerDictionarySnapshot
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.Scope
import me.apomazkin.per_dictionary_components.deps.PerDictionaryComponentsUseCase
import me.apomazkin.polytrainer.mapper.toDomain
import javax.inject.Inject

/**
 * Business-impl `PerDictionaryComponentsUseCase` (IS481, M13).
 *
 * - `flowComponentsForDictionary` — scoped subset (global ∪ per-dict для словаря).
 * - Write-методы (`create` / `rename` / `previewDeletionImpact` / `softDelete`) —
 *   делегирование на shared CRUD (решение Open Q #2 business_contract).
 *
 * F126 retrofit: `sharedCrud` параметр типа [ComponentsManagerUseCase] (interface),
 * не concrete impl — DIP. Dagger резолвит через `@Binds` в `ComponentsManagerModule`.
 */
class PerDictionaryComponentsUseCaseImpl @Inject constructor(
    private val lexemeApi: CoreDbApi.LexemeApi,
    private val sharedCrud: ComponentsManagerUseCase,
) : PerDictionaryComponentsUseCase {

    override fun flowComponentsForDictionary(
        dictionaryId: Long,
    ): Flow<PerDictionarySnapshot> =
        lexemeApi.flowUserDefinedTypesForDictionary(dictionaryId).map { api ->
            PerDictionarySnapshot(
                dictionaryId = api.dictionaryId,
                dictionaryName = api.dictionaryName,
                types = api.types.map { it.toDomain() },
                valueCountByType = api.valueCountByType.mapKeys { ComponentTypeId(it.key) },
            )
        }

    override suspend fun createUserDefinedComponent(
        name: String,
        template: ComponentTemplate,
        isMultiple: Boolean,
        scope: Scope,
    ): CreateOutcome = sharedCrud.createUserDefinedComponent(name, template, isMultiple, scope)

    override suspend fun renameComponent(
        typeId: ComponentTypeId,
        newName: String,
    ): RenameOutcome = sharedCrud.renameComponent(typeId, newName)

    override suspend fun previewDeletionImpact(
        typeId: ComponentTypeId,
    ): DeletionImpact? = sharedCrud.previewDeletionImpact(typeId)

    override suspend fun softDeleteComponent(
        typeId: ComponentTypeId,
    ): DeleteOutcome = sharedCrud.softDeleteComponent(typeId)

    override suspend fun editComponent(
        typeId: ComponentTypeId,
        name: String,
        template: ComponentTemplate,
        isMultiple: Boolean,
    ): EditOutcome = sharedCrud.editComponent(typeId, name, template, isMultiple)
}

