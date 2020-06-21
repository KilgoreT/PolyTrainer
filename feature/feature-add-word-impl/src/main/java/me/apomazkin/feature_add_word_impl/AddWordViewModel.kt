package me.apomazkin.feature_add_word_impl

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.WordWithDefinition
import javax.inject.Inject

class AddWordViewModel @Inject constructor(
    private val dbApi: CoreDbApi
) : ViewModel() {

    val data = MutableLiveData<List<WordWithDefinition>>()
    val addWord = MutableLiveData<String>()

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
        addWord.value?.let { word ->
            if (word.isBlank()) return
            dbApi
                .addWord(word)
            loadData()
        }
    }

    // TODO: 21.06.2020 убирать еще и definitions для данного слова
    fun removeWord(it: Long) {
        dbApi
            .removeWord(it)
    }

}