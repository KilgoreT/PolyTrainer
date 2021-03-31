package me.apomazkin.feature_statistic_impl.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.apomazkin.core_interactor.CoreInteractorApi
import me.apomazkin.feature_statistic_api.FeatureStatisticNavigation
import javax.inject.Inject

class FeatureViewModelFactory @Inject constructor(
    private val coreInteractorApi: CoreInteractorApi,
    private val navigation: FeatureStatisticNavigation,
//    private val delegate: LoadState
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(StatisticViewModel::class.java)) {
            StatisticViewModel(coreInteractorApi, navigation) as T
        } else {
            throw IllegalStateException("Cannot find ViewModel class")
        }
    }

}