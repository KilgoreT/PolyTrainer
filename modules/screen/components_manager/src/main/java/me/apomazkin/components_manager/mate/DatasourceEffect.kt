package me.apomazkin.components_manager.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.RecoverableEffect
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.Scope

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
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.CreateResult(epochId, CreateOutcome.Failure(error))
    }

    data class LoadImpact(
        val typeId: ComponentTypeId,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.ImpactPreviewFailed(typeId, error)
    }

    data class SoftDeleteComponent(
        val epochId: Long,
        val typeId: ComponentTypeId,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.DeleteResult(epochId, DeleteOutcome.Failure(error))
    }

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
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.EditResult(epochId, EditOutcome.Failure(error))
    }
}
