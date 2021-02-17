package me.apomazkin.feature_statistic_impl.ui

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.feature_statistic_api.FeatureStatisticNavigation
import me.apomazkin.feature_statistic_impl.domain.StatisticScenario
import javax.inject.Inject

class StatisticViewModel @Inject constructor(
    private val dbApi: CoreDbApi,
    private val navigation: FeatureStatisticNavigation,
    private val statisticScenario: StatisticScenario
//    private val loadStateDelegate: LoadState
) : ViewModel()/*, LoadState by loadStateDelegate*/ {


    val statInfo = MutableLiveData<String>()

    init {
        loadData()
    }

    @SuppressLint("CheckResult")
    private fun loadData() {
        statisticScenario.getStatistics()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result ->
                statInfo.postValue(result)
            }, {

            })
    }

}