package me.apomazkin.feature_vocabulary_impl

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.WordWithDefinition
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation
import javax.inject.Inject

class WordListViewModel @Inject constructor(
    private val dbApi: CoreDbApi,
    private val navigation: FeatureVocabularyNavigation
) : ViewModel() {

    val data = MutableLiveData<List<WordWithDefinition>>()

    init {
        loadData()
    }

    @SuppressLint("CheckResult")
    private fun loadData() {
        dbApi
            .getWordWithDefinition()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    data.postValue(result)
                },
                {

                }
            )
    }

    fun addWord() {
        navigation.addWordDialog()
    }

    @SuppressLint("CheckResult")
    fun removeWord(it: Long) {
        dbApi
            .deleteWord(it)
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

}