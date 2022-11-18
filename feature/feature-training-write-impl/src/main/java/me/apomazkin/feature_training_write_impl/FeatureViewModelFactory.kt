package me.apomazkin.feature_training_write_impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.apomazkin.core_interactor.CoreInteractorApi
import me.apomazkin.feature_training_write_api.FeatureTrainingWriteNavigator
import me.apomazkin.feature_training_write_impl.ui.TrainingWriteViewModel
import javax.inject.Inject

class FeatureViewModelFactory @Inject constructor(
    private val coreInteractorApi: CoreInteractorApi,
    private val navigation: FeatureTrainingWriteNavigator,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(TrainingWriteViewModel::class.java)) {
            TrainingWriteViewModel(coreInteractorApi, navigation) as T
        } else {
            throw IllegalStateException("Cannot find ViewModel class")
        }
    }

}