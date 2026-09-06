package me.apomazkin.dictionary.list

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.apomazkin.dictionary.DictionaryUseCase
import io.github.kilgoret.mate.MateFlowHandler
import javax.inject.Inject

class DictionaryListFlowHandler @Inject constructor(
    private val dictionaryUseCase: DictionaryUseCase,
) : MateFlowHandler<DictionaryListMsg> {

    override var job: Job? = null

    override fun subscribe(scope: CoroutineScope, send: (DictionaryListMsg) -> Unit) {
        job = scope.launch {
            dictionaryUseCase.flowDictionaryList()
                .collectLatest { list ->
                    send(DictionaryListMsg.DictionariesLoaded(list))
                }
        }
    }
}
