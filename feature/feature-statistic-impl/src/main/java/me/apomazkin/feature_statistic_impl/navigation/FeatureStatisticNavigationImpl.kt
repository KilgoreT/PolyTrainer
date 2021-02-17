package me.apomazkin.feature_statistic_impl.navigation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import me.apomazkin.feature_statistic_api.FeatureStatisticNavigation
import me.apomazkin.feature_statistic_impl.R
import javax.inject.Inject

class FeatureStatisticNavigationImpl @Inject constructor(
    private val featureContainer: ViewGroup
) : FeatureStatisticNavigation {
    override fun start() {

        val featureView = LayoutInflater.from(featureContainer.context)
            .inflate(R.layout.feature_container, featureContainer, false)
        featureContainer.addView(featureView)
        val navController = featureView.findNavController()
        navController.setGraph(R.navigation.feature_statistic)
    }
}