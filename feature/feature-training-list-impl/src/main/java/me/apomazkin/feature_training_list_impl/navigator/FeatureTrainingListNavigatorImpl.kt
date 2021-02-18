package me.apomazkin.feature_training_list_impl.navigator

import androidx.navigation.NavController
import me.apomazkin.feature_training_list_api.FeatureTrainingListNavigator
import me.apomazkin.feature_training_list_impl.R
import javax.inject.Inject

class FeatureTrainingListNavigatorImpl @Inject constructor(
    private val navController: NavController
) : FeatureTrainingListNavigator {
    override fun start() {
        navController.setGraph(R.navigation.feature_training_list)
    }
}