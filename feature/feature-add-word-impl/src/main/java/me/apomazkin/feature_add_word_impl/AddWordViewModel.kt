package me.apomazkin.feature_add_word_impl

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Word
import javax.inject.Inject

class AddWordViewModel @Inject constructor(
    private val dbApi: CoreDbApi
) : ViewModel() {
    val data = MutableLiveData<List<Word>>()

    init {
        loadData()
    }

    @SuppressLint("CheckResult")
    private fun loadData() {
        dbApi
            .getWordList()
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

    fun addData() {
        val time = System.currentTimeMillis()
        dbApi
            .insert("qwerty $time")
        loadData()
    }

}