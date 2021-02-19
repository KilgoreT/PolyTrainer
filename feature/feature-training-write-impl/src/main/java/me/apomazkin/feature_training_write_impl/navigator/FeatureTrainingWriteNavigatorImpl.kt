package me.apomazkin.feature_training_write_impl.navigator

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import me.apomazkin.feature_training_write_api.FeatureTrainingWriteNavigator
import me.apomazkin.feature_training_write_impl.R
import javax.inject.Inject

class FeatureTrainingWriteNavigatorImpl @Inject constructor(
    private val navController: NavController
) : FeatureTrainingWriteNavigator {
    override fun start() {
        val tempGraph = navController.navInflater.inflate(R.navigation.feature_training_write)
        val node = tempGraph.findNode(R.id.fragmentTrainingWrite)
        node?.let {
            it.label = "zhopa"
//            navController.ac
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build()
            tempGraph.remove(node)
            navController.graph.addDestination(it)
            navController.navigate(R.id.fragmentTrainingWrite)
        }
//        navController.setGraph(R.navigation.feature_training_write)
    }
}