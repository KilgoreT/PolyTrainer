package me.apomazkin.feature_add_word_impl.navigator

import androidx.navigation.NavController
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation
import me.apomazkin.feature_vocabulary_impl.R
import javax.inject.Inject

class FeatureVocabularyNavigationImpl @Inject constructor(
    private val navController: NavController
) : FeatureVocabularyNavigation {
    override fun start() {
        navController.setGraph(R.navigation.navigation)
    }
}