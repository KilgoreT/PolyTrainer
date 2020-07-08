package me.apomazkin.feature_vocabulary_impl.navigator

import androidx.navigation.NavController
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation
import me.apomazkin.feature_vocabulary_impl.R
import me.apomazkin.feature_vocabulary_impl.ui.wordList.WordListFragmentDirections
import javax.inject.Inject

class FeatureVocabularyNavigationImpl @Inject constructor(
    private val navController: NavController
) : FeatureVocabularyNavigation {
    override fun start() {
        navController.setGraph(R.navigation.navigation)
    }

    override fun addWordDialog() {
        navController.navigate(R.id.action_listWordFragment_to_addWordDialog)
    }

    override fun closeDialog() {
        navController.popBackStack()
    }

    override fun addDefinitionDialog(id: Long) {
        val action = WordListFragmentDirections.actionListWordFragmentToAddDefinitionDialog(id)
        navController.navigate(action)
    }
}