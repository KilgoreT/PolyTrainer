package me.apomazkin.vocabulary.logic

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateFlowHandler
import me.apomazkin.vocabulary.deps.VocabularyHostUseCase
import javax.inject.Inject

/**
 * IS493 Э2 (D9.1): long-running подписка host'а на текущий словарь.
 * Каждая эмиссия (включая null — «словарей нет», IS476) → [Msg.DictionaryChanged].
 * Эффектов у host'а нет — [runEffect] no-op.
 */
class CurrentDictFlowHandler @Inject constructor(
    private val useCase: VocabularyHostUseCase,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        job = scope.launch {
            useCase.flowCurrentDictId().collectLatest { dictId ->
                send(Msg.DictionaryChanged(dictionaryId = dictId))
            }
        }
    }

    override suspend fun runEffect(
        effect: Effect,
        consumer: (Msg) -> Unit,
    ) = Unit
}
