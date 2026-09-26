package me.apomazkin.per_dictionary_components.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.RecoverableEffect
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.OptionOutcome
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.SetEnabledOutcome

/**
 * Datasource Effects для `PerDictionaryComponentsScreen`. См. business_design_tree.md #42.
 *
 * Живой список компонентов эффектом не выражается: это подписка
 * [PerDictionaryComponentsSub.Components], декларируемая из state.
 *
 * F124/F136: write effects несут `epochId` для correlation reducer'ом с активным
 * диалогом. `LoadImpact` несёт `typeId` как correlation token.
 */
sealed interface DatasourceEffect : Effect {
    data class CreateComponent(
        val epochId: Long,
        val name: String,
        val template: ComponentTemplate,
        val isMultiple: Boolean,
        val scope: Scope,
        // IS486: цель зависимости + стартовые варианты CHOICE.
        val target: DependencyTarget = DependencyTarget.Lexeme,
        val core: Boolean = true,
        val optionLabels: List<String> = emptyList(),
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

    // Загрузка/перезагрузка списка компонентов эффектом не выражается:
    // это длящаяся подписка [PerDictionaryComponentsSub.Components],
    // retry рестартует её generation-полем через дифф раннера.

    /**
     * Phase 2 (IS481): edit existing user-defined component_type. Зеркало Manager
     * `EditComponent` (multi-dict picker отсутствует в PerDict).
     */
    data class EditComponent(
        val epochId: Long,
        val typeId: ComponentTypeId,
        val name: String,
        val template: ComponentTemplate,
        val isMultiple: Boolean,
        // IS486: перепривязка цели + батч опций CHOICE (rename изменённых, add новых).
        // Опции применяются ТОЛЬКО при `EditOutcome.Success` основного edit'а.
        val target: DependencyTarget = DependencyTarget.Lexeme,
        val core: Boolean = true,
        val optionRenames: List<Pair<Long, String>> = emptyList(),
        val optionAdds: List<String> = emptyList(),
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.EditResult(epochId, EditOutcome.Failure(error))
    }

    /** IS486: рубильник enabled (spec §6). Correlation — typeId. */
    data class SetEnabled(
        val typeId: ComponentTypeId,
        val enabled: Boolean,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.SetEnabledResult(typeId, SetEnabledOutcome.Failure(error))
    }

    /** IS486 (В2): preview impact удаления опции для вложенного конфирма. */
    data class LoadOptionImpact(
        val optionId: Long,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.OptionImpactFailed(optionId, error)
    }

    /** IS486 умный сброс: preview impact перепривязки (конфирм перед применением). */
    data class LoadRebindImpact(
        val typeId: ComponentTypeId,
        val target: DependencyTarget,
        val core: Boolean,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.RebindImpactFailed(typeId, error)
    }

    /** IS486 (В2): немедленное удаление опции из Edit-диалога (после конфирма). */
    data class DeleteOption(
        val epochId: Long,
        val optionId: Long,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.OptionDeleteResult(epochId, OptionOutcome.Failure(error))
    }
}
