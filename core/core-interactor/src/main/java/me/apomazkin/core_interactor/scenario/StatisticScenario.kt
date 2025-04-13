package me.apomazkin.core_interactor.scenario

//import me.apomazkin.core_interactor.useCase.statistic.GetDefinitionCountUseCase
//import me.apomazkin.core_interactor.useCase.statistic.GetWordClassCountUseCase
//import me.apomazkin.core_interactor.useCase.statistic.GetWordCountUseCase
import javax.inject.Inject

interface StatisticScenario {
    //    fun getWordClassCountInfo(langId: Long): Single<String>
    //    fun getWriteQuizInto(langId: Long): Single<String>

    class Impl @Inject constructor(
        //        private val getWordCountUseCase: GetWordCountUseCase,
        //        private val getDefinitionCountUseCase: GetDefinitionCountUseCase,
        //        private val getWordClassCountUseCase: GetWordClassCountUseCase,
    ) : StatisticScenario {
        
        //        override fun getWordClassCountInfo(langId: Long): Single<String> {
        //            return getWordCountUseCase.exec(langId)
        //                .zipWith(
        //                    getDefinitionCountUseCase.exec(),
        //                    BiFunction { words, definitions ->
        //                        return@BiFunction "Words: $words\nDefinitions: $definitions\n\n"
        //                    }
        //                )
        //                .zipWith(
        //                    getWordClassCountUseCase.getCount("noun"),
        //                    { accumulator, noun -> return@zipWith accumulator.plus("Noun: $noun\n") }
        //                )
        //                .zipWith(
        //                    getWordClassCountUseCase.getCount("verb"),
        //                    { accumulator, verb -> return@zipWith accumulator.plus("Verb: $verb\n") }
        //                )
        //                .zipWith(
        //                    getWordClassCountUseCase.getCount("adjective"),
        //                    { accumulator, adj -> return@zipWith accumulator.plus("Adjective: $adj\n") }
        //                )
        //                .zipWith(
        //                    getWordClassCountUseCase.getCount("adverb"),
        //                    { accumulator, adv -> return@zipWith accumulator.plus("Adverb: $adv\n") }
        //                )
        //                .flatMap { Single.just(it) }
        //        }
        
        //        override fun getWriteQuizInto(langId: Long): Single<String> {
        //            return getWriteQuizCountUseCase.getCount(0, langId)
        //                .zipWith(
        //                    getWriteQuizCountUseCase.getCount(1, langId),
        //                    BiFunction { tier0, tier1 ->
        //                        return@BiFunction "Write Quiz Info: \n0: $tier0\n1: $tier1\n"
        //                    }
        //                )
        //                .zipWith(
        //                    getWriteQuizCountUseCase.getCount(2, langId),
        //                    BiFunction { accumulator, tier2 ->
        //                        return@BiFunction accumulator.plus("2: $tier2")
        //                    }
        //                )
        //        }

    }
}