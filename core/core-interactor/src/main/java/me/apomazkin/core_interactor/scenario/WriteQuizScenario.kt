package me.apomazkin.core_interactor.useCase.writeQuiz

import io.reactivex.Completable
import io.reactivex.Single
import me.apomazkin.core_interactor.entity.WriteQuizStep
import me.apomazkin.core_interactor.useCase.definition.GetDefinitionUseCase
import me.apomazkin.core_interactor.useCase.word.GetWordUseCase
import javax.inject.Inject
import kotlin.math.min

interface WriteQuizScenario {
    fun getWriteQuizStepList(): Single<List<WriteQuizStep>>
    fun updateWriteQuizStep(writeQuizStep: WriteQuizStep): Completable
}

class WriteQuizScenarioImpl @Inject constructor(
    private val getWriteQuizUseCase: GetWriteQuizUseCase,
    private val updateWriteQuizUseCase: UpdateWriteQuizUseCase,
    private val getDefinitionUseCase: GetDefinitionUseCase,
    private val getWordUseCase: GetWordUseCase
) : WriteQuizScenario {

    override fun getWriteQuizStepList(): Single<List<WriteQuizStep>> {

        return getQuizStepList(0, 10)
            .flatMap { grade0 ->
                getQuizStepList(1, 10)
                    .flatMap { grade1 ->
                        getQuizStepList(2, 10)
                            .flatMap { grade2 ->

                                val mutualGrade0 = grade0.toMutableList()
                                val mutualGrade1 = grade1.toMutableList()
                                val mutualGrade2 = grade2.toMutableList()

                                // TODO: 25.03.2021 не понял, что за херня с l0,
                                //  но оно типа потом недоступно:
                                //  Unable to evaluate the expression Method
                                //  threw 'java.util.ConcurrentModificationException' exception.
                                val l0 = mutualGrade0.subList(0, min(7, mutualGrade0.size))
                                val l0Copy = l0.toList()
                                mutualGrade0.removeAll(l0Copy)

                                val l1 = mutualGrade1.subList(0, min(2, mutualGrade1.size))
                                val l1Copy = l1.toList()
                                mutualGrade1.removeAll(l1Copy)

                                val l2 = mutualGrade2.subList(0, min(1, mutualGrade2.size))
                                val l2Copy = l2.toList()
                                mutualGrade2.removeAll(l2Copy)

                                val result = mutableListOf<WriteQuizStep>()
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
                                result.shuffle()
                                Single.just(result)
                            }
                    }
            }
    }

    override fun updateWriteQuizStep(writeQuizStep: WriteQuizStep): Completable {
        return updateWriteQuizUseCase
            .updateWriteQuiz(writeQuizStep)
    }

    private fun getQuizStepList(grade: Int, limit: Int): Single<List<WriteQuizStep>> {
        return getWriteQuizUseCase
            .getWriteQuiz(grade, limit)
            .flattenAsObservable { list -> list.asIterable() }
            .flatMapSingle { writeQuiz ->
                getDefinitionUseCase
                    .getDefinition(writeQuiz.id)
                    .flatMap { definition ->
                        getWordUseCase.getWord(definition.wordId ?: -1L)
                            .map { word ->
                                WriteQuizStep(
                                    id = writeQuiz.id,
                                    definition = definition.value ?: "",
                                    definitionId = writeQuiz.definitionId,
                                    answer = word.value ?: "",
                                    grade = writeQuiz.grade,
                                    score = writeQuiz.score
                                )
                            }
                    }
            }
            .toList()
    }
}