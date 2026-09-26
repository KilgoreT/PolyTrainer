package me.apomazkin.wordcard.mate

import io.github.kilgoret.mate.MateSubscriptionHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.wordcard.LogTags
import me.apomazkin.wordcard.deps.WordCardUseCase
import java.text.Collator
import javax.inject.Inject

private const val TAG = "ComponentTypesFlow"

/**
 * Исполнитель семейства подписок [WordCardSub]: раннер mate отдаёт
 * сюда подписку, появившуюся в наборе [subscriptions], и коллектит
 * возвращённый Flow в mailbox; отмена коллекта при исчезновении
 * подписки — на раннере. Одна when-ветка = один вид подписки.
 *
 * Обработка ошибок различается по контракту веток:
 * - типы компонентов: `catch` → [Msg.ComponentTypesLoadFailed] —
 *   экран показывает снек с Retry;
 * - группы (word/dict): ошибка только логируется, fail-Msg нет —
 *   блок групп деградирует молча, рассинхрон derived-чипов
 *   выводится из логов с ID-списками.
 */
class WordCardSubHandler @Inject constructor(
    private val useCase: WordCardUseCase,
    private val logger: LexemeLogger,
) : MateSubscriptionHandler<Msg, WordCardSub> {

    override val subscriptionFamily = WordCardSub::class

    private val collator: Comparator<String> = run {
        val collator = Collator.getInstance()
        Comparator { a, b -> collator.compare(a, b) }
    }

    override fun flow(sub: WordCardSub): Flow<Msg> = when (sub) {
        is WordCardSub.ComponentTypes ->
            useCase.flowAvailableComponentTypes(sub.dictionaryId)
                .map<_, Msg> { available -> Msg.ComponentTypesLoaded(available) }
                .catch { t ->
                    if (t is CancellationException) throw t
                    logger.log(LogLevel.ERROR, TAG, "flowAvailableComponentTypes failed", t)
                    emit(Msg.ComponentTypesLoadFailed(t))
                }

        is WordCardSub.WordGroups ->
            useCase.wordGroups(sub.wordId)
                .map<_, Msg> { groups ->
                    val ids = groups.mapTo(LinkedHashSet()) { it.id }
                    logger.d(
                        tag = LogTags.WORDCARD,
                        message = "wordGroups: word=${sub.wordId} ids=$ids",
                    )
                    Msg.WordGroupsLoaded(ids)
                }
                .catch { t ->
                    if (t is CancellationException) throw t
                    logger.log(LogLevel.ERROR, LogTags.WORDCARD, "wordGroups failed", t)
                }

        is WordCardSub.DictGroups ->
            useCase.dictGroups(sub.dictionaryId)
                .map<_, Msg> { groups ->
                    val sorted = groups
                        .sortedWith(compareBy(collator) { it.name })
                        .map { GroupUi(id = it.id, name = it.name) }
                    logger.d(
                        tag = LogTags.WORDCARD,
                        message = "dictGroups: count=${sorted.size} ids=${sorted.map { it.id }}",
                    )
                    Msg.DictGroupsLoaded(sorted)
                }
                .catch { t ->
                    if (t is CancellationException) throw t
                    logger.log(LogLevel.ERROR, LogTags.WORDCARD, "dictGroups failed", t)
                }
    }
}
