package me.apomazkin.components_manager.mate

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.Scope
import me.apomazkin.mate.Effect

/**
 * Datasource Effects для `ComponentsManagerScreen`. См. business_contract_spec.md § IO.
 *
 * Initial subscribe реализован как `init`-trigger в `AllUserDefinedTypesFlowHandler.subscribe()`
 * (F088 — assisted FlowHandler стартует на init Mate). Отдельный `SubscribeAll` не нужен.
 *
 * F124/F136 retrofit: write-операции несут `epochId`, чтобы соответствующий `*Result`
 * Msg мог быть скоррелирован reducer'ом с активным диалогом.
 * `LoadImpact` несёт `typeId` (он же correlation token для preview).
 */
sealed interface DatasourceEffect : Effect {

    data class CreateComponent(
        val epochId: Long,
        val name: String,
        val template: ComponentTemplate,
        val isMultiple: Boolean,
        val scope: Scope,
    ) : DatasourceEffect

    data class RenameComponent(
        val epochId: Long,
        val typeId: ComponentTypeId,
        val newName: String,
    ) : DatasourceEffect

    data class LoadImpact(val typeId: ComponentTypeId) : DatasourceEffect

    data class SoftDeleteComponent(
        val epochId: Long,
        val typeId: ComponentTypeId,
    ) : DatasourceEffect

    /**
     * F163: эмитится reducer'ом на `Msg.OnRetryClick` для re-подписки на
     * `useCase.flowAllUserDefinedTypes()` после initial load failure.
     * Handler — [AllUserDefinedTypesFlowHandler] (отменяет существующую job и
     * стартует новую через `subscribe()`).
     */
    data object LoadAllUserDefinedTypes : DatasourceEffect

    /**
     * Phase 2 (IS481): edit existing user-defined component_type. UseCaseImpl
     * выполняет name validation + template-immutability gate перед API call.
     */
    data class EditComponent(
        val epochId: Long,
        val typeId: ComponentTypeId,
        val name: String,
        val template: ComponentTemplate,
        val isMultiple: Boolean,
    ) : DatasourceEffect

    /**
     * Phase 2 (IS481): re-subscribe trigger для `DictionariesFlowHandler`
     * (parity с F163 / LoadAllUserDefinedTypes).
     */
    data object SubscribeDictionaries : DatasourceEffect
}
