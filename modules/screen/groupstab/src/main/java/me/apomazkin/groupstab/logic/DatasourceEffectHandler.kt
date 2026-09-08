package me.apomazkin.groupstab.logic

import io.github.kilgoret.mate.MateEffectHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.groupstab.LogTags
import me.apomazkin.groupstab.deps.GroupsTabUseCase
import me.apomazkin.logger.LexemeLogger
import javax.inject.Inject

/**
 * Исполнитель эффектов-МУТАЦИЙ вкладки «Группы»: разовые намерения
 * (создать/переименовать/удалить группу), которые reducer выдаёт
 * эффектом, а раннер mate роутит сюда по семейству [GroupsEffect].
 * Каждый эффект — один вызов use case → доменный outcome →
 * [toMutationMsg] (маппинг в плоский Msg ДО отправки — конвенция);
 * `runCatching`-guard (T-6) → [Msg.GroupMutationFailed].
 *
 * Живые потоки данных (slice, окна, тикер) — НЕ здесь: они длящиеся
 * и декларируются подписками [GroupsSub] + [GroupsSubHandler].
 */
class DatasourceEffectHandler @Inject constructor(
    private val useCase: GroupsTabUseCase,
    private val logger: LexemeLogger,
) : MateEffectHandler<Msg, GroupsEffect> {

    override val effectFamily = GroupsEffect::class

    override suspend fun runEffect(
        effect: GroupsEffect,
        consumer: (Msg) -> Unit,
    ) {
        logger.d(tag = LogTags.GROUPS, message = "effect: $effect")
        val msg: Msg = when (val eff = effect) {
            is GroupsEffect.CreateGroup -> withContext(Dispatchers.IO) {
                runCatching { useCase.createGroup(eff.dictionaryId, eff.name) }
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

            is GroupsEffect.RenameGroup -> withContext(Dispatchers.IO) {
                runCatching { useCase.renameGroup(eff.groupId, eff.name) }
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

            is GroupsEffect.DeleteGroup -> withContext(Dispatchers.IO) {
                runCatching { useCase.deleteGroup(eff.groupId) }
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

            is GroupsEffect.DeleteGroupWithWords -> withContext(Dispatchers.IO) {
                runCatching { useCase.deleteGroupWithWords(eff.groupId) }
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
