package me.apomazkin.wordcard.mate

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import io.github.kilgoret.mate.MateFlowHandler
import me.apomazkin.wordcard.LogTags
import me.apomazkin.wordcard.deps.WordCardUseCase
import java.text.Collator
import javax.inject.Inject

/**
 * IS493 Э5 (D22.4): живые подписки блока групп карточки — отдельный
 * flow-handler (прецедент AvailableComponentTypesFlowHandler: wordId/
 * dictionaryId на старте движка неизвестны, триггер — эффект
 * [DatasourceEffect.SubscribeGroupBlock] из ветки WordLoaded).
 *
 * Подписки:
 * - `wordGroups(wordId)` → id-set членств → [Msg.WordGroupsLoaded];
 * - `dictGroups(dictionaryId)` → Collator-сортировка → [Msg.DictGroupsLoaded].
 * Логи — С ID-СПИСКАМИ (ревью Test-1): рассинхрон derived-чипов
 * выводим из лога.
 */
class GroupBlockFlowHandler @Inject constructor(
    private val useCase: WordCardUseCase,
    private val logger: LexemeLogger,
) : MateFlowHandler<Msg> {

    override var job: Job? = null
    private var scope: CoroutineScope? = null
    private var send: ((Msg) -> Unit)? = null

    private val collator: Comparator<String> = run {
        val collator = Collator.getInstance()
        Comparator { a, b -> collator.compare(a, b) }
    }

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        this.scope = scope
        this.send = send
    }

    fun resubscribe(subscribe: DatasourceEffect.SubscribeGroupBlock) {
        val scope = scope ?: return
        val send = send ?: return
        job?.cancel()
        // UNDISPATCHED: подписки регистрируются синхронно до возврата
        // runEffect (первая эмиссия не теряется — прецедент §9.4).
        job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    useCase.wordGroups(subscribe.wordId).collect { groups ->
                        val ids = groups.mapTo(LinkedHashSet()) { it.id }
                        logger.d(
                            tag = LogTags.WORDCARD,
                            message = "wordGroups: word=${subscribe.wordId} ids=$ids",
                        )
                        send(Msg.WordGroupsLoaded(ids))
                    }
                } catch (c: CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    logger.log(LogLevel.ERROR, LogTags.WORDCARD, "wordGroups failed", t)
                }
            }
            launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    useCase.dictGroups(subscribe.dictionaryId).collect { groups ->
                        val sorted = groups
                            .sortedWith(compareBy(collator) { it.name })
                            .map { GroupUi(id = it.id, name = it.name) }
                        logger.d(
                            tag = LogTags.WORDCARD,
                            message = "dictGroups: count=${sorted.size} ids=${sorted.map { it.id }}",
                        )
                        send(Msg.DictGroupsLoaded(sorted))
                    }
                } catch (c: CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    logger.log(LogLevel.ERROR, LogTags.WORDCARD, "dictGroups failed", t)
                }
            }
        }
    }
}
