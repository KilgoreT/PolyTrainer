package me.apomazkin.polytrainer.di.module.perdictionarycomponents

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.OptionCrudOutcome
import me.apomazkin.core_db_api.entity.SetEnabledComponentOutcome
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
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.per_dictionary_components.LogTags
import me.apomazkin.per_dictionary_components.deps.PerDictionaryComponentsUseCase
import me.apomazkin.polytrainer.mapper.toDomain
import javax.inject.Inject

/**
 * Business-impl `PerDictionaryComponentsUseCase` (IS481, M13).
 *
 * - `flowComponentsForDictionary` — scoped subset (global ∪ per-dict для словаря);
 *   IS486: builtin включены + optionsByType.
 * - Write-методы (`create` / `previewDeletionImpact` / `softDelete` / `edit`) —
 *   делегирование на shared CRUD (решение Open Q #2 business_contract).
 * - IS486: `setComponentEnabled` + CRUD опций — прямые вызовы `lexemeApi`
 *   (методы существуют только у per-dict конструктора), try-catch → Failure.
 *
 * F126 retrofit: `sharedCrud` параметр типа [ComponentsManagerUseCase] (interface),
 * не concrete impl — DIP. Dagger резолвит через `@Binds` в `ComponentsManagerModule`.
 */
class PerDictionaryComponentsUseCaseImpl @Inject constructor(
    private val lexemeApi: CoreDbApi.LexemeApi,
    private val sharedCrud: ComponentsManagerUseCase,
    private val logger: LexemeLogger,
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
                optionsByType = api.optionsByType
                    .mapKeys { ComponentTypeId(it.key) }
                    .mapValues { (_, options) -> options.map { it.toDomain() } },
            )
        }

    override suspend fun createUserDefinedComponent(
        name: String,
        template: ComponentTemplate,
        isMultiple: Boolean,
        scope: Scope,
        core: Boolean,
        dependsOnTypeId: Long?,
        dependsOnOptionId: Long?,
        optionLabels: List<String>,
    ): CreateOutcome = sharedCrud.createUserDefinedComponent(
        name = name,
        template = template,
        isMultiple = isMultiple,
        scope = scope,
        core = core,
        dependsOnTypeId = dependsOnTypeId,
        dependsOnOptionId = dependsOnOptionId,
        optionLabels = optionLabels,
    )

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
        core: Boolean,
        dependsOnTypeId: Long?,
        dependsOnOptionId: Long?,
    ): EditOutcome = sharedCrud.editComponent(
        typeId = typeId,
        name = name,
        template = template,
        isMultiple = isMultiple,
        core = core,
        dependsOnTypeId = dependsOnTypeId,
        dependsOnOptionId = dependsOnOptionId,
    )

    override suspend fun setComponentEnabled(
        typeId: ComponentTypeId,
        enabled: Boolean,
    ): SetEnabledOutcome = try {
        when (val r = lexemeApi.setComponentEnabled(typeId.id, enabled)) {
            is SetEnabledComponentOutcome.Success -> SetEnabledOutcome.Success(r.type.toDomain())
            SetEnabledComponentOutcome.LastEnabledCore -> SetEnabledOutcome.LastEnabledCore
            SetEnabledComponentOutcome.Removed -> SetEnabledOutcome.Removed
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.e(tag = LogTags.DICT_COMPONENTS, message = "setComponentEnabled failed: ${e.message}")
        SetEnabledOutcome.Failure(e)
    }

    override suspend fun addOption(
        typeId: ComponentTypeId,
        label: String,
    ): OptionOutcome = optionCall("addOption") { lexemeApi.addComponentOption(typeId.id, label) }

    override suspend fun renameOption(
        optionId: Long,
        label: String,
    ): OptionOutcome = optionCall("renameOption") { lexemeApi.renameComponentOption(optionId, label) }

    override suspend fun deleteOption(
        optionId: Long,
    ): OptionOutcome = optionCall("deleteOption") { lexemeApi.deleteComponentOption(optionId) }

    override suspend fun previewOptionDeletionImpact(
        optionId: Long,
    ): DeletionImpact? = try {
        lexemeApi.previewOptionDeletionImpact(optionId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.e(tag = LogTags.DICT_COMPONENTS, message = "previewOptionDeletionImpact failed: ${e.message}")
        null
    }

    override suspend fun previewRebindImpact(
        typeId: ComponentTypeId,
        core: Boolean,
        dependsOnTypeId: Long?,
        dependsOnOptionId: Long?,
    ): DeletionImpact? = try {
        lexemeApi.previewRebindImpact(
            typeId = typeId.id,
            core = core,
            dependsOnTypeId = dependsOnTypeId,
            dependsOnOptionId = dependsOnOptionId,
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.e(tag = LogTags.DICT_COMPONENTS, message = "previewRebindImpact failed: ${e.message}")
        null
    }

    private suspend fun optionCall(
        op: String,
        call: suspend () -> OptionCrudOutcome,
    ): OptionOutcome = try {
        when (val r = call()) {
            is OptionCrudOutcome.Success -> OptionOutcome.Success(r.option.toDomain())
            is OptionCrudOutcome.Deleted -> OptionOutcome.Deleted(r.impact)
            OptionCrudOutcome.Removed -> OptionOutcome.Removed
            OptionCrudOutcome.BuiltInProtected -> OptionOutcome.BuiltInProtected
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.e(tag = LogTags.DICT_COMPONENTS, message = "$op failed: ${e.message}")
        OptionOutcome.Failure(e)
    }
}
