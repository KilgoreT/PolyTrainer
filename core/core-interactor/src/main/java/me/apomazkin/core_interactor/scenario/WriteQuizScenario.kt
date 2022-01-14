package me.apomazkin.core_interactor.scenario

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import me.apomazkin.core_db_api.entity.WriteQuiz
import me.apomazkin.core_interactor.LangGod
import me.apomazkin.core_interactor.useCase.statistic.GetWriteQuizCountUseCase
import me.apomazkin.core_interactor.useCase.writeQuiz.GetWriteQuizByAccessTimeUseCase
import me.apomazkin.core_interactor.useCase.writeQuiz.GetWriteQuizByRandomUseCase
import me.apomazkin.core_interactor.useCase.writeQuiz.UpdateWriteQuizUseCase
import javax.inject.Inject
import kotlin.math.min

interface WriteQuizScenario {
    /**
     * Retrieve list of quizzes.
     */
    fun getWriteQuizList(langId: Long): Single<List<WriteQuiz>>

    /**
     * Update score, access etc info for current Quiz.
     * @param writeQuiz current quiz.
     */
    fun updateWriteQuiz(writeQuiz: WriteQuiz): Completable

    class Impl @Inject constructor(
        private val getWriteQuizByRandomUseCase: GetWriteQuizByRandomUseCase,
        private val getWriteQuizByAccessTimeUseCase: GetWriteQuizByAccessTimeUseCase,
        private val updateWriteQuizUseCase: UpdateWriteQuizUseCase,
        private val getWriteQuizCountUseCase: GetWriteQuizCountUseCase,
    ) : WriteQuizScenario {

        override fun getWriteQuizList(langId: Long): Single<List<WriteQuiz>> {

            return getCountOfFirstTier(langId)
                .zipWith(
                    getQuizListByRandom(0, 10, langId),
                    BiFunction { firstTierCount, firstTierList ->
                        return@BiFunction Pair(firstTierCount, firstTierList)
                    })
                .flatMap { grade0 ->
                    getQuizListByRandom(1, 10, langId)
                        .flatMap { grade1 ->
                            getQuizListByAccessTime(2, 10, langId)
                                .flatMap { grade2 ->

                                    val mutualGrade0 = grade0.second.shuffled().toMutableList()
                                    val mutualGrade1 = grade1.shuffled().toMutableList()
                                    val mutualGrade2 = grade2.shuffled().toMutableList()

                                    // TODO: 25.03.2021 не понял, что за херня с l0,
                                    //  но оно типа потом недоступно:
                                    //  Unable to evaluate the expression Method
                                    //  threw 'java.util.ConcurrentModificationException' exception.
                                    val l0 =
                                        mutualGrade0.subList(
                                            0,
                                            min(grade0.first, mutualGrade0.size)
                                        )
                                    val l0Copy = l0.toList()
                                    mutualGrade0.removeAll(l0Copy)

                                    val l1 = mutualGrade1.subList(
                                        0,
                                        min(8 - grade0.first, mutualGrade1.size)
                                    )
                                    val l1Copy = l1.toList()
                                    mutualGrade1.removeAll(l1Copy)

                                    val l2 = mutualGrade2.subList(0, min(2, mutualGrade2.size))
                                    val l2Copy = l2.toList()
                                    mutualGrade2.removeAll(l2Copy)

                                    val result = mutableListOf<WriteQuiz>()
                                    result.addAll(l0Copy)
                                    result.addAll(l1Copy)
                                    result.addAll(l2Copy)

                                    if (result.size < 10) {
                                        val needed = 10 - result.size
                                        result.addAll(
                                            mutualGrade0.subList(
                                                0,
                                                min(needed, mutualGrade0.size)
                                            )
                                        )
                                    }
                                    if (result.size < 10) {
                                        val needed = 10 - result.size
                                        result.addAll(
                                            mutualGrade1.subList(
                                                0,
                                                min(needed, mutualGrade1.size)
                                            )
                                        )
                                    }
                                    if (result.size < 10) {
                                        val needed = 10 - result.size
                                        result.addAll(
                                            mutualGrade2.subList(
                                                0,
                                                min(needed, mutualGrade2.size)
                                            )
                                        )
                                    }

                                    if (result.size < 10) {
                                        throw RuntimeException("Need More Word!")
                                    }
                                    Single.just(result.shuffled())
                                }
                        }
                }
                .onErrorResumeNext { ttt ->
                    Single.error(RuntimeException("last: ${ttt.message}"))
                }
        }

        private fun getCountOfFirstTier(langId: Long): Single<Int> {
            return getWriteQuizCountUseCase
                .getCount(0, LangGod.langId)
                .zipWith(getWriteQuizCountUseCase.getCount(1, langId), BiFunction { first, second ->
                    return@BiFunction getProportionOfFirst(first, second)
                })

        }

        private fun getProportionOfFirst(first: Int, second: Int): Int {
            val total = first + second
            val firstPercent = first.toFloat() / total * 100
            val res = 8 / 100F * firstPercent
            return res.toInt()
        }

        override fun updateWriteQuiz(writeQuiz: WriteQuiz): Completable {
            return updateWriteQuizUseCase
                .updateWriteQuiz(writeQuiz)
        }

        private fun getQuizListByRandom(
            grade: Int,
            limit: Int,
            langId: Long
        ): Single<List<WriteQuiz>> {
            return getWriteQuizByRandomUseCase
                .getRandomWriteQuiz(grade, limit, langId)
        }

        private fun getQuizListByAccessTime(
            grade: Int,
            limit: Int,
            langId: Long
        ): Single<List<WriteQuiz>> {
            return getWriteQuizByAccessTimeUseCase
                .getWriteQuizByAccessTime(grade, limit, langId)
        }
    }
}