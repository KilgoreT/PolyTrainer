package me.apomazkin.per_dictionary_components.mate

import io.github.kilgoret.mate.MateEffectHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.OptionOutcome
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.per_dictionary_components.LogTags
import me.apomazkin.per_dictionary_components.deps.PerDictionaryComponentsUseCase

/**
 * Маппер `DatasourceEffect` → `UseCase` call → `Msg.*Result`. Все IO через [Dispatchers.IO].
 *
 * Ошибок handler не ловит: провал каждого эффекта объявлен в его типе
 * ([io.github.kilgoret.mate.RecoverableEffect] → typed Failure
 * outcome), раннер доставит его сам; стектрейс логирует
 * ErrorLoggingObserver. Best-effort ошибки батча опций (не валящие
 * outcome) логируются здесь.
 *
 * F124/F136: write effects несут `epochId` (createDialog/renameDialog/deleteConfirm
 * session id); reducer применяет Result только если `epochId` совпадает с активным
 * dialog.epochId. `LoadImpact` несёт `typeId` как correlation token.
 *
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
class DatasourceEffectHandler(
    private val useCase: PerDictionaryComponentsUseCase,
    private val logger: LexemeLogger,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : MateEffectHandler<Msg, DatasourceEffect> {
    override val effectFamily = DatasourceEffect::class

    override suspend fun runEffect(
        effect: DatasourceEffect,
        consumer: (Msg) -> Unit,
    ) {
        val msg: Msg = withContext(io) {
            when (effect) {
                is DatasourceEffect.CreateComponent ->
                    Msg.CreateResult(
                        epochId = effect.epochId,
                        outcome = useCase.createUserDefinedComponent(
                            name = effect.name,
                            template = effect.template,
                            isMultiple = effect.isMultiple,
                            scope = effect.scope,
                            core = effect.core,
                            dependsOnTypeId = (effect.target as? DependencyTarget.Component)?.typeId?.id,
                            dependsOnOptionId = (effect.target as? DependencyTarget.Option)?.optionId,
                            optionLabels = effect.optionLabels,
                        ),
                    )

                is DatasourceEffect.LoadImpact -> {
                    // F145: null → ImpactPreviewFailed без synthetic exception
                    // (distinct semantics: "useCase returned null" vs "exception thrown").
                    val impact = useCase.previewDeletionImpact(effect.typeId)
                    if (impact != null) {
                        Msg.ImpactPreviewLoaded(typeId = effect.typeId, impact = impact)
                    } else {
                        Msg.ImpactPreviewFailed(typeId = effect.typeId, cause = null)
                    }
                }

                is DatasourceEffect.SoftDeleteComponent ->
                    Msg.DeleteResult(
                        epochId = effect.epochId,
                        outcome = useCase.softDeleteComponent(effect.typeId),
                    )

                is DatasourceEffect.EditComponent -> {
                    val outcome = useCase.editComponent(
                        typeId = effect.typeId,
                        name = effect.name,
                        template = effect.template,
                        isMultiple = effect.isMultiple,
                        core = effect.core,
                        dependsOnTypeId = (effect.target as? DependencyTarget.Component)?.typeId?.id,
                        dependsOnOptionId = (effect.target as? DependencyTarget.Option)?.optionId,
                    )
                    // IS486 (В2): батч опций — только при Success основного edit'а.
                    // Ошибки отдельных опций best-effort: логируются, не валят outcome
                    // (flow пере-отдаст фактическое состояние списка).
                    if (outcome is EditOutcome.Success) {
                        effect.optionRenames.forEach { (optionId, label) ->
                            val r = useCase.renameOption(optionId, label)
                            if (r !is OptionOutcome.Success) {
                                logger.e(
                                    tag = LogTags.DICT_COMPONENTS,
                                    message = "renameOption($optionId) failed: $r",
                                )
                            }
                        }
                        effect.optionAdds.forEach { label ->
                            val r = useCase.addOption(effect.typeId, label)
                            if (r !is OptionOutcome.Success) {
                                logger.e(
                                    tag = LogTags.DICT_COMPONENTS,
                                    message = "addOption failed: $r",
                                )
                            }
                        }
                    }
                    Msg.EditResult(epochId = effect.epochId, outcome = outcome)
                }

                is DatasourceEffect.SetEnabled ->
                    Msg.SetEnabledResult(
                        typeId = effect.typeId,
                        outcome = useCase.setComponentEnabled(effect.typeId, effect.enabled),
                    )

                is DatasourceEffect.LoadOptionImpact -> {
                    val impact = useCase.previewOptionDeletionImpact(effect.optionId)
                    if (impact != null) {
                        Msg.OptionImpactLoaded(optionId = effect.optionId, impact = impact)
                    } else {
                        Msg.OptionImpactFailed(optionId = effect.optionId, cause = null)
                    }
                }

                is DatasourceEffect.LoadRebindImpact -> {
                    val impact = useCase.previewRebindImpact(
                        typeId = effect.typeId,
                        core = effect.core,
                        dependsOnTypeId = (effect.target as? DependencyTarget.Component)?.typeId?.id,
                        dependsOnOptionId = (effect.target as? DependencyTarget.Option)?.optionId,
                    )
                    if (impact != null) {
                        Msg.RebindImpactLoaded(typeId = effect.typeId, impact = impact)
                    } else {
                        Msg.RebindImpactFailed(typeId = effect.typeId, cause = null)
                    }
                }

                is DatasourceEffect.DeleteOption ->
                    Msg.OptionDeleteResult(
                        epochId = effect.epochId,
                        outcome = useCase.deleteOption(effect.optionId),
                    )
            }
        }
        consumer(msg)
    }
}
