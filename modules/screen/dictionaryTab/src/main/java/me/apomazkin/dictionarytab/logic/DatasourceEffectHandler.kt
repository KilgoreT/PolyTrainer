package me.apomazkin.dictionarytab.logic

import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.apomazkin.dictionarytab.deps.DictionaryTabUseCase
import me.apomazkin.dictionarytab.entity.WordInfo
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateFlowHandler
import me.apomazkin.mate.LogTags
import me.apomazkin.logger.LexemeLogger
import javax.inject.Inject

sealed interface DatasourceEffect : Effect {

    data class LoadTermFlow(
            val pattern: String = EMPTY_STRING,
    ) : DatasourceEffect

    data class CreateWord(val value: String) : DatasourceEffect
    data class UpdateWord(val wordId: Long, val value: String) : DatasourceEffect

    data class RemoveWords(val wordSet: Set<WordInfo>) : DatasourceEffect
}

class DatasourceEffectHandler @Inject constructor(
        private val dictionaryTabUseCase: DictionaryTabUseCase,
        private val logger: LexemeLogger,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null
    private var pagingScope: CoroutineScope? = null

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        pagingScope = scope
        scope.launch {
            // IS476: flowCurrentDict() теперь Flow<DictUiEntity?> — null проходит
            // как валидное доменное состояние, reducer обработает в Msg.SelectDictionary.
            dictionaryTabUseCase.flowCurrentDict().collectLatest { dict ->
                send(Msg.SelectDictionary(current = dict))
            }
        }
    }

    override suspend fun runEffect(
            effect: Effect,
            consumer: (Msg) -> Unit,
    ) {
        logger.d(tag = LogTags.MATE, message = "RunEffect: $effect")
        val msg = when (val eff = effect as? DatasourceEffect) {
            is DatasourceEffect.LoadTermFlow -> withContext(Dispatchers.IO) {
                // IS476: getCurrentDict() теперь nullable — страхуемся на случай race,
                // когда reducer уже отфильтровал null, но эффект мог быть "в пути".
                val dictionaryId = dictionaryTabUseCase.getCurrentDict()?.id?.toInt()
                if (dictionaryId == null) {
                    Msg.NoOperation
                } else {
                    val pagingFlow = dictionaryTabUseCase.searchTerms(
                            pattern = eff.pattern,
                            dictionaryId = dictionaryId,
                    ).let { flow ->
                        val scope = pagingScope
                        if (eff.pattern.isEmpty() && scope != null) flow.cachedIn(scope) else flow
                    }
                    Msg.TermsLoaded(
                            pattern = eff.pattern,
                            termList = pagingFlow,
                    )
                }
            }

            is DatasourceEffect.CreateWord -> withContext(Dispatchers.IO) {
                dictionaryTabUseCase.addWord(eff.value)
                Msg.NoOperation
            }

            is DatasourceEffect.UpdateWord -> withContext(Dispatchers.IO) {
                async { dictionaryTabUseCase.updateWord(eff.wordId, eff.value) }.await()
                Msg.NoOperation
            }

            is DatasourceEffect.RemoveWords -> {
                withContext(Dispatchers.IO) {
                    eff.wordSet.map { id ->
                        async { dictionaryTabUseCase.deleteWord(id.id) }.await()
                    }
                }
                Msg.NoOperation
            }
            null -> return
        }
        consumer(msg)
    }
}
