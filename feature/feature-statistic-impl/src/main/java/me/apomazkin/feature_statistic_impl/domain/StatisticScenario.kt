package me.apomazkin.feature_statistic_impl.domain

import io.reactivex.Single
import io.reactivex.functions.BiFunction
import javax.inject.Inject

interface StatisticScenario {
    fun getStatistics(): Single<String>

    class StatisticScenarioImpl @Inject constructor(
        private val wordCountUseCase: WordCountUseCase,
        private val definitionCountUseCase: DefinitionCountUseCase,
        private val nounCountUseCase: NounCountUseCase,
        private val verbCountUseCase: VerbCountUseCase,
        private val adjectiveCountUseCase: AdjectiveCountUseCase,
        private val adverbCountUseCase: AdverbCountUseCase
    ) : StatisticScenario {
        override fun getStatistics(): Single<String> {
            return wordCountUseCase.exec()
                .zipWith(
                    definitionCountUseCase.exec(),
                    BiFunction { words, definitions ->
                        return@BiFunction "Words: $words\nDefinitions: $definitions\n\n"
                    }
                )
                .zipWith(
                    nounCountUseCase.exec(),
                    { accum, noun -> return@zipWith accum.plus("Noun: $noun\n") }
                )
                .zipWith(
                    verbCountUseCase.exec(),
                    { accum, verb -> return@zipWith accum.plus("Verb: $verb\n") }
                )
                .zipWith(
                    adjectiveCountUseCase.exec(),
                    { accum, adj -> return@zipWith accum.plus("Adjective: $adj\n") }
                )
                .zipWith(
                    adverbCountUseCase.exec(),
                    { accum, adv -> return@zipWith accum.plus("Adverb: $adv\n") }
                )
                .flatMap { Single.just(it) }
        }
    }
}