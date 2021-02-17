package me.apomazkin.feature_statistic_impl.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.feature_statistic_api.FeatureStatisticNavigation
import me.apomazkin.feature_statistic_impl.domain.StatisticScenario
import javax.inject.Inject

class FeatureViewModelFactory @Inject constructor(
    private val dbApi: CoreDbApi,
    private val navigation: FeatureStatisticNavigation,
    private val scenario: StatisticScenario
//    private val delegate: LoadState
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(StatisticViewModel::class.java)) {
            StatisticViewModel(dbApi, navigation, scenario) as T
        } else {
            throw IllegalStateException("Cannot find ViewModel class")
        }
    }

}