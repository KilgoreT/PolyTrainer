package me.apomazkin.feature_bottom_menu_impl.navigator

import androidx.navigation.NavController
import me.apomazkin.feature_bottom_menu_api.FeatureBottomMenuNavigator
import me.apomazkin.feature_bottom_menu_impl.R
import javax.inject.Inject

class FeatureBottomMenuNavigatorImpl @Inject constructor(
    private val navController: NavController
) : FeatureBottomMenuNavigator {

    override fun start() {
        navController.setGraph(R.navigation.bottom_menu_navigation)
    }
}