package me.apomazkin.groupstab.logic

import io.github.kilgoret.mate.MateEffectHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.groupstab.LogTags
import me.apomazkin.groupstab.deps.GroupsTabUseCase
import me.apomazkin.logger.LexemeLogger

/**
 * Исполнитель эффектов-МУТАЦИЙ вкладки «Группы»: разовые намерения
 * (создать/переименовать/удалить группу), которые reducer выдаёт
 * эффектом, а раннер mate роутит сюда по семейству [GroupsEffect].
 * Каждый эффект — один вызов use case → доменный outcome →
 * [toMutationMsg] (маппинг в плоский Msg ДО отправки — конвенция).
 *
 * Ошибок handler не ловит: провал объявлен в самом эффекте
 * ([io.github.kilgoret.mate.RecoverableEffect] →
 * [Msg.GroupMutationFailed]), раннер доставит его сам; стектрейс
 * логирует ErrorLoggingObserver.
 *
 * Живые потоки данных (slice, окна, тикер) — НЕ здесь: они длящиеся
 * и декларируются подписками [GroupsSub] + [GroupsSubHandler].
 *
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
class DatasourceEffectHandler(
    private val useCase: GroupsTabUseCase,
    private val logger: LexemeLogger,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : MateEffectHandler<Msg, GroupsEffect> {
    override val effectFamily = GroupsEffect::class

    override suspend fun runEffect(
        effect: GroupsEffect,
        consumer: (Msg) -> Unit,
    ) {
        logger.d(tag = LogTags.GROUPS, message = "effect: $effect")
        val msg: Msg = withContext(io) {
            when (effect) {
                is GroupsEffect.CreateGroup ->
                    useCase.createGroup(effect.dictionaryId, effect.name).toMutationMsg()

                is GroupsEffect.RenameGroup ->
                    useCase.renameGroup(effect.groupId, effect.name).toMutationMsg()

                is GroupsEffect.DeleteGroup ->
                    Msg.DeleteOutcomeMsg(useCase.deleteGroup(effect.groupId))

                is GroupsEffect.DeleteGroupWithWords ->
                    Msg.DeleteWithWordsOutcomeMsg(useCase.deleteGroupWithWords(effect.groupId))
            }
        }
        // Лог outcome'а (D32: NotFound обязан отличаться в логе от
        // пропавшего эффекта).
        logger.d(tag = LogTags.GROUPS, message = "outcome: $msg")
        consumer(msg)
    }
}
