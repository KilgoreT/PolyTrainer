package me.apomazkin.groupstab.logic

import io.github.kilgoret.mate.MateSubscriptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.apomazkin.group.buildDisplayTree
import me.apomazkin.groupstab.LogTags
import me.apomazkin.groupstab.deps.GroupsTabUseCase
import me.apomazkin.logger.LexemeLogger
import java.text.Collator
import javax.inject.Inject

/**
 * Исполнитель семейства подписок [GroupsSub]: раннер mate отдаёт сюда
 * каждую подписку, появившуюся в наборе [subscriptions], и собирает
 * из возвращённого Flow сообщения в общий mailbox; когда подписка
 * исчезает из набора — раннер отменяет коллект сам.
 *
 * Одна when-ветка = один вид подписки. Ошибки потока перехватываются
 * на месте (`catch` → fail-Msg): упавший источник переводит экран в
 * состояние ошибки, а не роняет раннер. Msg окон групп несут groupId
 * своей подписки — reducer no-op'ит эмиссию, не совпавшую с текущим
 * state (защита от гонки на границе рестарта подписки).
 *
 * Slice: `combine(membershipSlice, groupTree)` с distinctUntilChanged
 * обеих (мутация групп инвалидирует оба запроса — без дедупа дерево
 * пересобиралось бы дважды) → [buildDisplayTree]; сиблинги —
 * locale-aware Collator.
 */
class GroupsSubHandler @Inject constructor(
    private val useCase: GroupsTabUseCase,
    private val logger: LexemeLogger,
) : MateSubscriptionHandler<Msg, GroupsSub> {

    override val subFamily = GroupsSub::class

    private val collator: Comparator<String> = run {
        val collator = Collator.getInstance()
        Comparator { a, b -> collator.compare(a, b) }
    }

    override fun flow(sub: GroupsSub): Flow<Msg> = when (sub) {
        GroupsSub.CurrentDict ->
            useCase.flowCurrentDictId()
                .map { dictId -> Msg.DictionaryChanged(dictionaryId = dictId) }

        is GroupsSub.Slice ->
            combine(
                useCase.membershipSlice(sub.dictionaryId).distinctUntilChanged(),
                useCase.groupTree(sub.dictionaryId).distinctUntilChanged(),
            ) { slice, groups ->
                logger.d(
                    tag = LogTags.GROUPS,
                    message = "slice: dict=${sub.dictionaryId} words=${slice.size} groups=${groups.size}",
                )
                Msg.SliceLoaded(
                    tree = buildDisplayTree(
                        slice = slice,
                        groups = groups,
                        comparator = collator,
                    ),
                ) as Msg
            }.catch { e ->
                logger.e(tag = LogTags.GROUPS, message = "slice failed: $e")
                emit(Msg.SliceLoadFailed)
            }

        is GroupsSub.AllWindow ->
            useCase.flowWordsWindow(dictionaryId = sub.dictionaryId, limit = sub.limit)
                .map<_, Msg> { words ->
                    logger.d(
                        tag = LogTags.GROUPS,
                        message = "window: dict=${sub.dictionaryId} limit=${sub.limit} loaded=${words.size}",
                    )
                    Msg.WindowLoaded(words = words)
                }
                .catch { e ->
                    logger.e(tag = LogTags.GROUPS, message = "window failed: $e")
                    emit(Msg.WindowLoadFailed)
                }

        is GroupsSub.DeleteCountdown ->
            flow {
                while (true) {
                    delay(1_000)
                    emit(Msg.DeleteCountdownTick)
                }
            }

        is GroupsSub.GroupWindow ->
            useCase.flowGroupWordsWindow(groupId = sub.groupId, limit = sub.limit)
                .map<_, Msg> { words ->
                    logger.d(
                        tag = LogTags.GROUPS,
                        message = "window(group=${sub.groupId}): limit=${sub.limit} loaded=${words.size}",
                    )
                    Msg.GroupWindowLoaded(groupId = sub.groupId, words = words)
                }
                .catch { e ->
                    logger.e(
                        tag = LogTags.GROUPS,
                        message = "window(group=${sub.groupId}) failed: $e",
                    )
                    emit(Msg.GroupWindowFailed(groupId = sub.groupId))
                }
    }
}
