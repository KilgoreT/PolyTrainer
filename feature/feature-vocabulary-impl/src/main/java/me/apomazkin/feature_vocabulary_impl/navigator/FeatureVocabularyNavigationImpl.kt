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

    override fun editWordDialog(wordId: Long, currentValue: String) {
        val action = WordListFragmentDirections
            .actionListWordFragmentToEditWordDialog(wordId, currentValue)
        navController.navigate(action)
    }

    override fun closeDialog() {
        navController.popBackStack()
    }

    override fun editDefinitionDialog(id: Long) {
        val action = WordListFragmentDirections
            .actionListWordFragmentToEditDefinitionDialog(id)
        navController.navigate(action)
    }

    override fun addSampleDialog(definitionId: Long) {
        val action = WordListFragmentDirections
            .actionListWordFragmentToAddSampleDialog(definitionId)
        navController.navigate(action)
    }

    override fun addDefinitionDialog(id: Long) {
        val action = WordListFragmentDirections
            .actionListWordFragmentToAddDefinitionDialog(id)
        navController.navigate(action)
    }
}