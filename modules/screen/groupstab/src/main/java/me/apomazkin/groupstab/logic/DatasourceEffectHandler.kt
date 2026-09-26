package me.apomazkin.groupstab.logic

import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.runSuspendCatching
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
 * [toMutationMsg] (маппинг в плоский Msg ДО отправки — конвенция);
 * guard через [runSuspendCatching] (сбой → [Msg.GroupMutationFailed],
 * отмена корутины пробрасывается).
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
        val msg: Msg = when (val eff = effect) {
            is GroupsEffect.CreateGroup -> withContext(io) {
                runSuspendCatching { useCase.createGroup(eff.dictionaryId, eff.name) }
                    .fold(
                        onSuccess = {
                            logger.d(tag = LogTags.GROUPS, message = "create outcome: $it")
                            it.toMutationMsg()
                        },
                        onFailure = { e ->
                            logger.e(tag = LogTags.GROUPS, message = "create failed: $e")
                            Msg.GroupMutationFailed
                        },
                    )
            }

            is GroupsEffect.RenameGroup -> withContext(io) {
                runSuspendCatching { useCase.renameGroup(eff.groupId, eff.name) }
                    .fold(
                        onSuccess = {
                            logger.d(tag = LogTags.GROUPS, message = "rename outcome: $it")
                            it.toMutationMsg()
                        },
                        onFailure = { e ->
                            logger.e(tag = LogTags.GROUPS, message = "rename failed: $e")
                            Msg.GroupMutationFailed
                        },
                    )
            }

            is GroupsEffect.DeleteGroup -> withContext(io) {
                runSuspendCatching { useCase.deleteGroup(eff.groupId) }
                    .fold(
                        onSuccess = {
                            logger.d(tag = LogTags.GROUPS, message = "delete outcome: $it")
                            Msg.DeleteOutcomeMsg(it)
                        },
                        onFailure = { e ->
                            logger.e(tag = LogTags.GROUPS, message = "delete failed: $e")
                            Msg.GroupMutationFailed
                        },
                    )
            }

            is GroupsEffect.DeleteGroupWithWords -> withContext(io) {
                runSuspendCatching { useCase.deleteGroupWithWords(eff.groupId) }
                    .fold(
                        onSuccess = {
                            // Лог обоих исходов (D32: NotFound обязан
                            // отличаться в логе от пропавшего эффекта).
                            logger.d(
                                tag = LogTags.GROUPS,
                                message = "delete-with-words outcome: $it",
                            )
                            Msg.DeleteWithWordsOutcomeMsg(it)
                        },
                        onFailure = { e ->
                            logger.e(
                                tag = LogTags.GROUPS,
                                message = "delete-with-words failed: $e",
                            )
                            Msg.GroupMutationFailed
                        },
                    )
            }
        }
        consumer(msg)
    }
}
