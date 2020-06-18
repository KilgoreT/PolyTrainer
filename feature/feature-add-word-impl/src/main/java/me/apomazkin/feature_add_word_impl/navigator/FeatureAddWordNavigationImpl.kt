package me.apomazkin.feature_add_word_impl.navigator

import androidx.navigation.NavController
import me.apomazkin.feature_add_word_api.FeatureAddWordNavigation
import me.apomazkin.feature_add_word_impl.R
import javax.inject.Inject

class FeatureAddWordNavigationImpl @Inject constructor(
    private val navController: NavController
) : FeatureAddWordNavigation {
    override fun start() {
        navController.setGraph(R.navigation.navigation)
    }
}