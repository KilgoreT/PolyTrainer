package me.apomazkin.per_dictionary_components.mate

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.Scope
import me.apomazkin.mate.Effect

/**
 * Datasource Effects для `PerDictionaryComponentsScreen`. См. business_design_tree.md #42.
 *
 * Initial subscribe реализован как `init`-trigger в `ComponentsForDictionaryFlowHandler.subscribe()`
 * (assisted FlowHandler с dictionaryId). Отдельный `SubscribeForDictionary` Effect не нужен.
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
    ) : DatasourceEffect

    data class LoadImpact(val typeId: ComponentTypeId) : DatasourceEffect

    data class SoftDeleteComponent(
        val epochId: Long,
        val typeId: ComponentTypeId,
    ) : DatasourceEffect

    /**
     * F163: эмитится reducer'ом на `Msg.OnRetryClick` для re-подписки на
     * `useCase.flowComponentsForDictionary(dictionaryId)` после initial load failure.
     * Handler — [ComponentsForDictionaryFlowHandler] (отменяет существующую job и
     * стартует новую через `subscribe()`).
     */
    data object LoadComponentsForDictionary : DatasourceEffect

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
    ) : DatasourceEffect

    /** IS486: рубильник enabled (spec §6). Correlation — typeId. */
    data class SetEnabled(
        val typeId: ComponentTypeId,
        val enabled: Boolean,
    ) : DatasourceEffect

    /** IS486 (В2): preview impact удаления опции для вложенного конфирма. */
    data class LoadOptionImpact(val optionId: Long) : DatasourceEffect

    /** IS486 умный сброс: preview impact перепривязки (конфирм перед применением). */
    data class LoadRebindImpact(
        val typeId: ComponentTypeId,
        val target: DependencyTarget,
        val core: Boolean,
    ) : DatasourceEffect

    /** IS486 (В2): немедленное удаление опции из Edit-диалога (после конфирма). */
    data class DeleteOption(
        val epochId: Long,
        val optionId: Long,
    ) : DatasourceEffect
}
