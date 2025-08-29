package me.apomazkin.dictionarytab.logic

import android.util.Log
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.apomazkin.dictionarytab.BuildConfig
import me.apomazkin.dictionarytab.deps.DictionaryTabUseCase
import me.apomazkin.dictionarytab.entity.WordInfo
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateFlowHandler

/**
 * Effect
 */
internal sealed interface DatasourceEffect : Effect {

    /**
     * Effect to load data.
     */
    data class LoadTermFlow(
            val pattern: String = EMPTY_STRING,
    ) : DatasourceEffect

    /**
     * Effect to create new word.
     */
    data class CreateWord(val value: String) : DatasourceEffect
    data class UpdateWord(val wordId: Long, val value: String) : DatasourceEffect

    /**
     * Effect to remove words.
     */
    data class RemoveWords(val wordSet: Set<WordInfo>) : DatasourceEffect
}

/**
 * EffectHandler for datastore calls.
 */
internal class DatasourceEffectHandler(
        private val dictionaryTabUseCase: DictionaryTabUseCase,
        private val scope: CoroutineScope,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        scope.launch {
            dictionaryTabUseCase.flowCurrentDict().collectLatest {
                send(Msg.SelectDictionary(current = it))
            }
        }
    }

    override suspend fun runEffect(
            effect: Effect,
            consumer: (Msg) -> Unit,
    ) {
        //TODO kilg 14.06.2025 00:48
        // https://github.com/KilgoreT/PolyTrainer/issues/372
        if (BuildConfig.DEBUG) {
            Log.d("##MATE##", "RunEffect: $effect")
        }
        return when (val eff = effect as? DatasourceEffect) {

            //TODO kilg 25.05.2025 03:16 В префах хранить именно id текущего языка,
            // а не numericCode. И вообще, кешировать бы его, а не запрашивать каждый раз.
            // https://github.com/KilgoreT/PolyTrainer/issues/369
            is DatasourceEffect.LoadTermFlow -> {
                withContext(Dispatchers.IO) {
                    // TODO: зачем запрашивать id, если оно есть в стейте.
                    val langId = dictionaryTabUseCase.getLangId(
                            numericCode = dictionaryTabUseCase.getCurrentDict().numericCode
                    )
                    val pagingFlow = dictionaryTabUseCase.searchTerms(
                            pattern = eff.pattern,
                            langId = langId
                    ).let { flow ->
                        if (eff.pattern.isEmpty()) flow.cachedIn(scope) else flow
                    }
                    Msg.TermsLoaded(
                            pattern = eff.pattern,
                            termList = pagingFlow,
                    )

                }
            }

            is DatasourceEffect.CreateWord -> {
                withContext(Dispatchers.IO) {
                    dictionaryTabUseCase
                            .addWord(eff.value)
                            .let { Msg.NoOperation }
                }
            }

            is DatasourceEffect.UpdateWord -> {
                withContext(Dispatchers.IO) {
                    async {
                        dictionaryTabUseCase.updateWord(
                                eff.wordId,
                                eff.value
                        )
                    }
                            .await()
                            .let { Msg.NoOperation }
                }
            }

            is DatasourceEffect.RemoveWords -> {
                withContext(Dispatchers.IO) {
                    eff.wordSet.map { id ->
                        async { dictionaryTabUseCase.deleteWord(id.id) }.await()
                    }
                }.let { Msg.NoOperation }
            }
            null -> Msg.NoOperation
        }.let(consumer)
    }
}