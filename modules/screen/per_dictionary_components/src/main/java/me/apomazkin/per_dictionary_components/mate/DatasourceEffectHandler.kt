package me.apomazkin.per_dictionary_components.mate

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateTypedEffectHandler
import me.apomazkin.per_dictionary_components.LogTags
import me.apomazkin.per_dictionary_components.deps.PerDictionaryComponentsUseCase
import javax.inject.Inject

/**
 * Маппер `DatasourceEffect` → `UseCase` call → `Msg.*Result`. Все IO через [Dispatchers.IO].
 *
 * F125: `CancellationException` пробрасывается без wrap'а — структурная отмена
 * (job cancellation, navigation away) должна распространяться. Прочие throwables
 * → typed Failure outcome + logger.e.
 *
 * F124/F136: write effects несут `epochId` (createDialog/renameDialog/deleteConfirm
 * session id); reducer применяет Result только если `epochId` совпадает с активным
 * dialog.epochId. `LoadImpact` несёт `typeId` как correlation token.
 */
class DatasourceEffectHandler @Inject constructor(
    private val useCase: PerDictionaryComponentsUseCase,
    private val logger: LexemeLogger,
) : MateTypedEffectHandler<Msg, DatasourceEffect>() {

    override fun filter(effect: Effect): DatasourceEffect? {
        // F163: LoadComponentsForDictionary — обрабатывается
        // `ComponentsForDictionaryFlowHandler` (re-subscribe), не этим handler'ом.
        if (effect is DatasourceEffect.LoadComponentsForDictionary) return null
        return effect as? DatasourceEffect
    }

    override suspend fun onEffect(effect: DatasourceEffect, consumer: (Msg) -> Unit) {
        val msg: Msg = withContext(Dispatchers.IO) {
            try {
                when (effect) {
                    is DatasourceEffect.CreateComponent ->
                        Msg.CreateResult(
                            epochId = effect.epochId,
                            outcome = useCase.createUserDefinedComponent(
                                effect.name,
                                effect.template,
                                effect.isMultiple,
                                effect.scope,
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

                    is DatasourceEffect.EditComponent ->
                        Msg.EditResult(
                            epochId = effect.epochId,
                            outcome = useCase.editComponent(
                                typeId = effect.typeId,
                                name = effect.name,
                                template = effect.template,
                                isMultiple = effect.isMultiple,
                            ),
                        )

                    // F163: filtered out в filter() выше — недостижимо, но
                    // нужно для exhaustive `when` (sealed interface).
                    DatasourceEffect.LoadComponentsForDictionary -> Msg.Empty
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(
                    tag = LogTags.DICT_COMPONENTS,
                    message = "Effect failed: ${effect::class.simpleName} — ${e.message}",
                )
                when (effect) {
                    is DatasourceEffect.CreateComponent ->
                        Msg.CreateResult(effect.epochId, CreateOutcome.Failure(e))

                    is DatasourceEffect.LoadImpact ->
                        Msg.ImpactPreviewFailed(effect.typeId, e)

                    is DatasourceEffect.SoftDeleteComponent ->
                        Msg.DeleteResult(effect.epochId, DeleteOutcome.Failure(e))

                    is DatasourceEffect.EditComponent ->
                        Msg.EditResult(effect.epochId, EditOutcome.Failure(e))

                    DatasourceEffect.LoadComponentsForDictionary -> Msg.Empty
                }
            }
        }
        consumer(msg)
    }
}
