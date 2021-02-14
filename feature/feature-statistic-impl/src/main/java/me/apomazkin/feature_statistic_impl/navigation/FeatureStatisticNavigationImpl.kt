package me.apomazkin.feature_statistic_impl.navigation

import android.view.ViewGroup
import me.apomazkin.feature_statistic_api.FeatureStatisticNavigation
import javax.inject.Inject

class FeatureStatisticNavigationImpl @Inject constructor(
    private val featureContainer: ViewGroup
) : FeatureStatisticNavigation {
    override fun start() {

//        val ff = LayoutInflater.from(featureContainer.context).inflate(R.layout.feature_container, featureContainer, false)
//        featureContainer.addView(ff)
//        println()
//        navController.setGraph(R.navigation.navigation)
    }
}