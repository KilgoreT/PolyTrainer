package me.apomazkin.feature_statistic_impl.ui

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.apomazkin.core_interactor.CoreInteractorApi
import me.apomazkin.feature_statistic_api.FeatureStatisticNavigation
import javax.inject.Inject

class StatisticViewModel @Inject constructor(
    private val coreInteractorApi: CoreInteractorApi,
    private val navigation: FeatureStatisticNavigation,
//    private val loadStateDelegate: LoadState
) : ViewModel()/*, LoadState by loadStateDelegate*/ {


    val statInfo = MutableLiveData<String>()
    val writeQuizInfo = MutableLiveData<String>()

    init {
        loadData()
    }

    @SuppressLint("CheckResult")
    private fun loadData() {
        coreInteractorApi
            .statisticScenario()
            .getWordClassCountInfo()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result ->
                statInfo.postValue(result)
            }, {

            })

        coreInteractorApi
            .statisticScenario()
            .getWriteQuizInto()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result ->
                writeQuizInfo.postValue(result)
            }, {

            })
    }

}