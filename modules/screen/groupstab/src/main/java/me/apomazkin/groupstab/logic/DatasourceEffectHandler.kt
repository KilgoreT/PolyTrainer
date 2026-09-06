package me.apomazkin.groupstab.logic

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.apomazkin.group.buildDisplayTree
import me.apomazkin.groupstab.LogTags
import me.apomazkin.groupstab.deps.GroupsTabUseCase
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateFlowHandler
import java.text.Collator
import javax.inject.Inject

/**
 * IS493 Э2/Э3 (D10 v3, D15.4): handler данных вкладки «Группы».
 *
 * Живые подписки:
 * - структура: `combine(membershipSlice, groupTree)` (обе с
 *   `distinctUntilChanged` — мутация групп инвалидирует оба запроса,
 *   без дедупа дерево пересобиралось бы дважды, ревью D-7) →
 *   [buildDisplayTree] → [Msg.SliceLoaded]; сиблинги сортируются
 *   locale-aware Collator'ом (А8; default locale — единый источник D12.3);
 * - контент окна «Все»: `combine(dictionaryId, window)` →
 *   `flowWordsWindow` (Э2, не тронуто);
 * - окна раскрытых ГРУПП (Э5, D21.4): карта groupId→limit, `merge`
 *   per-flow с индивидуальным catch — НЕ combine (ревью Mate-1: combine
 *   гейтит все окна за самым медленным и умирает от ошибки одного).
 *
 * Мутации Э3 — тупое выполнение эффектов: use case → доменный outcome →
 * [toMutationMsg] (маппинг в плоский Msg ДО отправки — конвенция);
 * `runCatching`-guard (T-6) → [Msg.GroupMutationFailed].
 */
class DatasourceEffectHandler @Inject constructor(
    private val useCase: GroupsTabUseCase,
    private val logger: LexemeLogger,
) : MateFlowHandler<Msg>,
    MateEffectHandler<Msg, GroupsEffect> {

    override val effectFamily = GroupsEffect::class

    override var job: Job? = null
    private val dictionaryId = MutableStateFlow<Long?>(null)
    private val window = MutableStateFlow<Int?>(null)

    /** Окна раскрытых групп: groupId → limit (Э5, D21.4). */
    private val groupWindows = MutableStateFlow<Map<Long, Int>>(emptyMap())

    /** Э6: job секундных тиков паузы осмысления деструктива. */
    private var countdownJob: Job? = null
    private var countdownScope: CoroutineScope? = null
    private var countdownSend: ((Msg) -> Unit)? = null

    private val collator: Comparator<String> = run {
        val collator = Collator.getInstance()
        Comparator { a, b -> collator.compare(a, b) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        countdownScope = scope
        countdownSend = send
        job = scope.launch {
            launch {
                dictionaryId
                    .filterNotNull()
                    .flatMapLatest { dictId ->
                        combine(
                            useCase.membershipSlice(dictId).distinctUntilChanged(),
                            useCase.groupTree(dictId).distinctUntilChanged(),
                        ) { slice, groups ->
                            logger.d(
                                tag = LogTags.GROUPS,
                                message = "slice: dict=$dictId words=${slice.size} groups=${groups.size}",
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
                    }
                    .collect { msg -> send(msg) }
            }
            launch {
                groupWindows
                    .flatMapLatest { map ->
                        if (map.isEmpty()) {
                            emptyFlow()
                        } else {
                            merge(
                                *map.map { (groupId, limit) ->
                                    useCase.flowGroupWordsWindow(groupId = groupId, limit = limit)
                                        .map<_, Msg> { words ->
                                            logger.d(
                                                tag = LogTags.GROUPS,
                                                message = "window(group=$groupId): limit=$limit loaded=${words.size}",
                                            )
                                            Msg.GroupWindowLoaded(groupId = groupId, words = words)
                                        }
                                        .catch { e ->
                                            logger.e(
                                                tag = LogTags.GROUPS,
                                                message = "window(group=$groupId) failed: $e",
                                            )
                                            emit(Msg.GroupWindowFailed(groupId = groupId))
                                        }
                                }.toTypedArray()
                            )
                        }
                    }
                    .collect { msg -> send(msg) }
            }
            launch {
                combine(dictionaryId, window) { dictId, limit -> dictId to limit }
                    .flatMapLatest { (dictId, limit) ->
                        if (dictId == null || limit == null) {
                            emptyFlow()
                        } else {
                            useCase.flowWordsWindow(dictionaryId = dictId, limit = limit)
                                .map<_, Msg> { words ->
                                    logger.d(
                                        tag = LogTags.GROUPS,
                                        message = "window: dict=$dictId limit=$limit loaded=${words.size}",
                                    )
                                    Msg.WindowLoaded(words = words)
                                }
                                .catch { e ->
                                    logger.e(tag = LogTags.GROUPS, message = "window failed: $e")
                                    emit(Msg.WindowLoadFailed)
                                }
                        }
                    }
                    .collect { msg -> send(msg) }
            }
        }
    }

    override suspend fun runEffect(
        effect: GroupsEffect,
        consumer: (Msg) -> Unit,
    ) {
        logger.d(tag = LogTags.GROUPS, message = "effect: $effect")
        val msg: Msg = when (val eff = effect) {
            is GroupsEffect.SubscribeSlice -> {
                dictionaryId.value = eff.dictionaryId
                return
            }

            is GroupsEffect.SetWindow -> {
                window.value = eff.limit
                return
            }

            is GroupsEffect.SetGroupWindow -> {
                groupWindows.value =
                    if (eff.limit == null) groupWindows.value - eff.groupId
                    else groupWindows.value + (eff.groupId to eff.limit)
                return
            }

            is GroupsEffect.ClearGroupWindows -> {
                groupWindows.value = emptyMap()
                return
            }

            // Э6: тики паузы осмысления — job с отменой (повторный старт
            // перезапускает; Cancel гасит; хвостовой тик глушит no-op
            // guard reducer'а).
            is GroupsEffect.StartDeleteCountdown -> {
                countdownJob?.cancel()
                val scope = countdownScope ?: return
                val send = countdownSend ?: return
                countdownJob = scope.launch {
                    repeat(eff.seconds) {
                        kotlinx.coroutines.delay(1_000)
                        send(Msg.DeleteCountdownTick)
                    }
                }
                return
            }

            is GroupsEffect.CancelDeleteCountdown -> {
                countdownJob?.cancel()
                countdownJob = null
                return
            }

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
