package me.apomazkin.core_interactor.scenario

import io.reactivex.Single
import io.reactivex.functions.BiFunction
import me.apomazkin.core_interactor.useCase.statistic.GetDefinitionCountUseCase
import me.apomazkin.core_interactor.useCase.statistic.GetWordClassCountUseCase
import me.apomazkin.core_interactor.useCase.statistic.GetWordCountUseCase
import me.apomazkin.core_interactor.useCase.statistic.GetWriteQuizCountUseCase
import javax.inject.Inject

interface StatisticScenario {
    fun getWordClassCountInfo(): Single<String>
    fun getWriteQuizInto(): Single<String>

    class Impl @Inject constructor(
        private val getWordCountUseCase: GetWordCountUseCase,
        private val getDefinitionCountUseCase: GetDefinitionCountUseCase,
        private val getWordClassCountUseCase: GetWordClassCountUseCase,
        private val getWriteQuizCountUseCase: GetWriteQuizCountUseCase,
    ) : StatisticScenario {

        override fun getWordClassCountInfo(): Single<String> {
            return getWordCountUseCase.exec()
                .zipWith(
                    getDefinitionCountUseCase.exec(),
                    BiFunction { words, definitions ->
                        return@BiFunction "Words: $words\nDefinitions: $definitions\n\n"
                    }
                )
                .zipWith(
                    getWordClassCountUseCase.getCount("noun"),
                    { accumulator, noun -> return@zipWith accumulator.plus("Noun: $noun\n") }
                )
                .zipWith(
                    getWordClassCountUseCase.getCount("verb"),
                    { accumulator, verb -> return@zipWith accumulator.plus("Verb: $verb\n") }
                )
                .zipWith(
                    getWordClassCountUseCase.getCount("adjective"),
                    { accumulator, adj -> return@zipWith accumulator.plus("Adjective: $adj\n") }
                )
                .zipWith(
                    getWordClassCountUseCase.getCount("adverb"),
                    { accumulator, adv -> return@zipWith accumulator.plus("Adverb: $adv\n") }
                )
                .flatMap { Single.just(it) }
        }

        override fun getWriteQuizInto(): Single<String> {
            return getWriteQuizCountUseCase.getCount(0)
                .zipWith(
                    getWriteQuizCountUseCase.getCount(1),
                    BiFunction { tier0, tier1 ->
                        return@BiFunction "Write Quiz Info: \n0: $tier0\n1: $tier1\n"
                    }
                )
                .zipWith(
                    getWriteQuizCountUseCase.getCount(2),
                    BiFunction { accumulator, tier2 ->
                        return@BiFunction accumulator.plus("2: $tier2")
                    }
                )
        }

    }
}