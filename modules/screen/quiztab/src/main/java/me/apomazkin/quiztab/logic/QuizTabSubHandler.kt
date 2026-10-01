package me.apomazkin.quiztab.logic

import io.github.kilgoret.mate.MateSubscriptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.quiz.QuizGroupOptions
import me.apomazkin.quiz.QuizTypes
import me.apomazkin.quiz.resolveQuizGroupState
import me.apomazkin.quiztab.LogTags
import me.apomazkin.quiztab.deps.QuizTabUseCase
import java.text.Collator
import javax.inject.Inject

/**
 * Исполнитель семейства подписок [QuizTabSub]: раннер mate отдаёт сюда
 * подписку из набора [subscriptions] и коллектит возвращённый Flow в
 * mailbox; отмена — диффом раннера.
 *
 * GroupOptions: текущий словарь → `flatMapLatest` (смена словаря сама
 * гасит старый combine) → combine(счётчики групп, счётчик «Все»,
 * pref набора) → воронка `resolveQuizGroupState` на КАЖДОМ эмите:
 * транзиент «новые счётчики + мёртвая группа в наборе» резолвится без
 * кадра невалидного пункта (группа просто выпадает). Ошибки потока перехватываются на месте
 * (`catch` → fail-Msg): упавший источник не роняет раннер, карточка
 * остаётся на рабочем дефолте.
 */
class QuizTabSubHandler @Inject constructor(
    private val useCase: QuizTabUseCase,
    private val logger: LexemeLogger,
) : MateSubscriptionHandler<Msg, QuizTabSub> {

    override val subscriptionFamily = QuizTabSub::class

    // Порядок групп пикера — locale-aware, как на вкладке «Группы»
    // (спека групп §5: Collator вызывающей стороны).
    private val collator: Comparator<String> = run {
        val collator = Collator.getInstance()
        Comparator { a, b -> collator.compare(a, b) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun flow(sub: QuizTabSub): Flow<Msg> = when (sub) {
        QuizTabSub.GroupOptions ->
            useCase
                .flowCurrentDictId()
                .distinctUntilChanged()
                .flatMapLatest { dictId ->
                    if (dictId == null) {
                        // «Словарей нет»: пунктов нет, карточка disabled.
                        flowOf(
                            Msg.GroupOptionsLoaded(
                                quizType = QuizTypes.CHAT,
                                dictionaryId = null,
                                options = QuizGroupOptions(
                                    isAllEligible = false,
                                    allWordCount = 0,
                                    groups = emptyList(),
                                ),
                                selectedGroupIds = emptySet(),
                                selectionInvalidated = false,
                            ) as Msg,
                        )
                    } else {
                        combine(
                            useCase.flowQuizGroupCounts(dictId).distinctUntilChanged(),
                            useCase.flowDictionaryQuizWordCount(dictId).distinctUntilChanged(),
                            useCase.flowGroupSelection(QuizTypes.CHAT, dictId).distinctUntilChanged(),
                        ) { counts, allCount, persisted ->
                            val resolved = resolveQuizGroupState(
                                groupCounts = counts.sortedWith(
                                    compareBy(collator) { it.name },
                                ),
                                dictionaryWordCount = allCount,
                                persistedGroupIds = persisted.ids,
                            )
                            val selected = resolved.selectedGroupIds
                            val selectedText = if (selected.isEmpty()) {
                                "all"
                            } else {
                                selected.sorted().joinToString(",")
                            }
                            logger.d(
                                tag = LogTags.QUIZ,
                                message = "groupOptions: dict=$dictId " +
                                    "groups=${resolved.options.groups.size} " +
                                    "eligible=${resolved.options.groups.count { it.isEligible }} " +
                                    "allCount=$allCount " +
                                    "selected=$selectedText " +
                                    "garbage=${persisted.hasGarbage}",
                            )
                            Msg.GroupOptionsLoaded(
                                quizType = QuizTypes.CHAT,
                                dictionaryId = dictId,
                                options = resolved.options,
                                selectedGroupIds = selected,
                                // Воронка выкинула группу или в pref мусор —
                                // reducer закрепит очищенный набор записью.
                                // Эхо записи даёт равенство: цикла нет.
                                selectionInvalidated = persisted.hasGarbage ||
                                    persisted.ids != selected,
                            ) as Msg
                        }
                    }
                }.catch { e ->
                    logger.e(tag = LogTags.QUIZ, message = "groupOptions: failed | $e")
                    emit(Msg.GroupOptionsFailed(quizType = QuizTypes.CHAT))
                }
    }
}
