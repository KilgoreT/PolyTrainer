package me.apomazkin.components_manager.mate

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.Scope
import io.github.kilgoret.mate.Effect

/**
 * Datasource Effects для `ComponentsManagerScreen`. См. business_contract_spec.md § IO.
 *
 * Живой список типов эффектом не выражается: это подписка
 * [ComponentsManagerSub.AllTypes], декларируемая из state.
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

    data class LoadImpact(val typeId: ComponentTypeId) : DatasourceEffect

    data class SoftDeleteComponent(
        val epochId: Long,
        val typeId: ComponentTypeId,
    ) : DatasourceEffect

    // Живые списки (типы, словари) эффектами не выражаются: это
    // подписки [ComponentsManagerSub], декларируемые из state; retry
    // рестартует упавшую подписку generation-полем через дифф раннера.

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
}
