package me.apomazkin.feature_training_list_impl.navigator

import androidx.navigation.NavController
import me.apomazkin.feature_training_list_api.FeatureTrainingListNavigator
import me.apomazkin.feature_training_list_impl.R
import javax.inject.Inject
import javax.inject.Named

class FeatureTrainingListNavigatorImpl @Inject constructor(
    @Named("current") private val currentController: NavController
) : FeatureTrainingListNavigator {
    override fun start() {
        currentController.setGraph(R.navigation.feature_training_list)
    }
}