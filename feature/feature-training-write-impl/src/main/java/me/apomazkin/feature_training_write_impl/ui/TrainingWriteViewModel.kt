package me.apomazkin.feature_training_write_impl.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Quiz
import me.apomazkin.feature_training_write_api.FeatureTrainingWriteNavigator
import javax.inject.Inject

// TODO: 28.02.2021 QUIZ
class TrainingWriteViewModel @Inject constructor(
    private val dbApi: CoreDbApi,
    private val navigation: FeatureTrainingWriteNavigator,
//    private val loadStateDelegate: LoadState
) : ViewModel()/*, LoadState by loadStateDelegate*/ {


    val currentQuiz = MutableLiveData<Int>()
    val currentQuizTitle = MutableLiveData<String>()
    val data = MutableLiveData<List<Quiz>>()

    init {
        loadData()
    }

    @SuppressLint("CheckResult")
    fun loadData() {
        dbApi
            .getRandomQuizList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { list ->
                    list.forEach { item -> Log.d("###", ">>>> :::: $item") }
                    data.postValue(list)
                },
                {
                    throw RuntimeException("Trololo")
                }
            )
    }

}