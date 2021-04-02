package me.apomazkin.feature_vocabulary_impl

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.apomazkin.core_db_api.entity.Term
import me.apomazkin.core_interactor.CoreInteractorApi
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation
import me.apomazkin.feature_vocabulary_impl.loadState.LoadState
import javax.inject.Inject

class WordListViewModel @Inject constructor(
    private val coreInteractorApi: CoreInteractorApi,
    private val navigation: FeatureVocabularyNavigation,
    private val loadStateDelegate: LoadState
) : ViewModel(), LoadState by loadStateDelegate {

    val data = MutableLiveData<List<Term>>()
    val wordPattern = MutableLiveData<String>("")
    val transition = MutableLiveData<String>()

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
                    .addWord(it)
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

    fun editDefinition(id: Long?) {
        id?.let {
            TODO("Not yet implemented")
        }
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
        }
    }

}