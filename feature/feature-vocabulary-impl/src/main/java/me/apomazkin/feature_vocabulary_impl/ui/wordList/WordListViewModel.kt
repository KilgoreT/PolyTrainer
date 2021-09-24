package me.apomazkin.feature_vocabulary_impl.ui.wordList

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import me.apomazkin.core_db_api.entity.Term
import me.apomazkin.core_db_api.entity.Word
import me.apomazkin.core_db_api.entity.WriteQuiz
import me.apomazkin.core_interactor.CoreInteractorApi
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation
import me.apomazkin.feature_vocabulary_impl.loadState.LoadState
import javax.inject.Inject

@SuppressLint("CheckResult")
class WordListViewModel @Inject constructor(
    private val coreInteractorApi: CoreInteractorApi,
    private val navigation: FeatureVocabularyNavigation,
    private val loadStateDelegate: LoadState
) : ViewModel(), LoadState by loadStateDelegate {

    val data = MutableLiveData<List<Term>>()
    val wordPattern = MutableLiveData<String>("")
    val transition = MutableLiveData<String>()
    val argh = MutableLiveData<String>()

    init {
//        loadData()
        wordPattern.observeForever {
            coreInteractorApi
                .searchTermUseCase()
                .getTermList("%$it%")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { result ->
                        if (result.isEmpty()) onEmpty() else onData()
                        data.postValue(result)
                    },
                    { onError(it) }
                )
        }
    }

    @SuppressLint("CheckResult")
    private fun loadData() {
        onLoad()
        coreInteractorApi
            .getTermUseCase()
            .getTermList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    if (result.isEmpty()) onEmpty() else onData()
                    data.postValue(result)
                },
                { onError(it) }
            )
    }

    fun addWord() {
        wordPattern.value?.let {
            if (it.isNotBlank()) {
                coreInteractorApi
                    .addWordUseCase()
                    .addWord(it.trim())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        wordPattern.postValue("")
                    }
            }
        }
        transition.postValue("START")

    }

    @SuppressLint("CheckResult")
    fun removeWord(id: Long) {
        coreInteractorApi
            .removeWordUseCase()
            .removeWord(id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                // TODO: 02.12.2020 реализовать обратную связь
            }, { error ->
                // TODO: 02.12.2020 реакция на ошибку
            })
    }

    fun addDefinition(id: Long?) {
        id?.let {
            navigation.addDefinitionDialog(it)
        }
    }

    fun editDefinition(id: Long) {
        navigation.editDefinitionDialog(id)
    }

    fun deleteDefinition(id: Long?) {
        id?.let {
            coreInteractorApi
                .removeDefinitionUseCase()
                .removeDefinition(it)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    // TODO: 02.12.2020 реализовать обратную связь
                }, { error ->
                    // TODO: 02.12.2020 реакция на ошибку
                })
            coreInteractorApi.removeWriteQuizUseCase()
                .removeWriteQuiz(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    // TODO: 02.12.2020 реализовать обратную связь
                }, { error ->
                    // TODO: 02.12.2020 реакция на ошибку
                })

        }
    }

    fun editWord(word: Word) {
        word.id?.let { id ->
            word.value?.let { value ->
                navigation.editWordDialog(id, value.trim())
            }
        }
    }

    fun addSample(definitionId: Long) {
        navigation.addSampleDialog(definitionId)
    }

    @SuppressLint("CheckResult")
    fun argh() {
        coreInteractorApi
            .getDefinitionUseCase()
            .getDefinition()
            .zipWith(
                coreInteractorApi
                    .getWriteQuizByAccessTimeUseCase()
                    .getWriteQuizList(), BiFunction { defList, quizList ->
                    val result = mutableListOf<WriteQuiz>()
                    quizList.forEach { quiz ->
                        val temp = defList.filter { item -> item.id == quiz.definition.id }
                        if (temp.isEmpty()) {
                            result.add(quiz)
                        }
                    }
                    return@BiFunction result
                })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { ttt ->
                argh.postValue("Wrong: ${ttt.size}")
            }
    }

    @SuppressLint("CheckResult")
    fun arghDelete() {
        coreInteractorApi
            .getDefinitionUseCase()
            .getDefinition()
            .zipWith(coreInteractorApi
                .getWriteQuizByAccessTimeUseCase()
                .getWriteQuizList(), BiFunction { defList, quizList ->
                val result = mutableListOf<WriteQuiz>()
                quizList.forEach { quiz ->
                    val temp = defList.filter { item -> item.id == quiz.definition.id }
                    if (temp.isEmpty()) {
                        result.add(quiz)
                    }
                }
                return@BiFunction result
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { ttt ->
                ttt.forEach { item ->
                    coreInteractorApi
                        .removeWriteQuizUseCase()
                        .removeWriteQuiz(item.definition.id ?: throw IllegalStateException())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                        }
                }
            }
    }

}