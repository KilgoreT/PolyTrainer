package me.apomazkin.components_manager.mate

import io.github.kilgoret.mate.MateEffectHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase

/**
 * Маппер `DatasourceEffect` → `UseCase` call → `Msg.*Result`. Все IO через [Dispatchers.IO].
 *
 * Ошибок handler не ловит: провал каждого эффекта объявлен в его типе
 * ([io.github.kilgoret.mate.RecoverableEffect] → typed Failure
 * outcome), раннер доставит его сам; стектрейс логирует
 * ErrorLoggingObserver.
 *
 * Correlation (F124/F136 retrofit):
 * - Write effects несут `epochId` (createDialog/renameDialog/deleteConfirm session id);
 *   reducer применяет Result только если `epochId` совпадает с активным dialog.epochId.
 * - `LoadImpact` несёт `typeId` как correlation token — Loaded/Failed Msg
 *   проверяются reducer'ом против активного `deleteConfirm.typeId`.
 *
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
class DatasourceEffectHandler(
    private val useCase: ComponentsManagerUseCase,
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
            }
        }
        consumer(msg)
    }
}
