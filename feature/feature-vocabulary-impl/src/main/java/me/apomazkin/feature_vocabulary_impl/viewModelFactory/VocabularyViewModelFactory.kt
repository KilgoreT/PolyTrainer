package me.apomazkin.feature_vocabulary_impl.viewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.apomazkin.core_interactor.CoreInteractorApi
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation
import me.apomazkin.feature_vocabulary_impl.loadState.LoadState
import me.apomazkin.feature_vocabulary_impl.ui.addDefinition.AddDefinitionViewModel
import me.apomazkin.feature_vocabulary_impl.ui.addSample.AddSampleViewModel
import me.apomazkin.feature_vocabulary_impl.ui.editDefinition.EditDefinitionViewModel
import me.apomazkin.feature_vocabulary_impl.ui.editWord.EditWordViewModel
import me.apomazkin.feature_vocabulary_impl.ui.wordList.WordListViewModel
import javax.inject.Inject

class VocabularyViewModelFactory @Inject constructor(
    private val coreInteractorApi: CoreInteractorApi,
    private val navigation: FeatureVocabularyNavigation,
    private val delegate: LoadState
) : ViewModelProvider.Factory {

    private var id: Long = -1

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(WordListViewModel::class.java)) {
            WordListViewModel(coreInteractorApi, navigation, delegate) as T
        } else if (modelClass.isAssignableFrom(EditWordViewModel::class.java)) {
            EditWordViewModel(coreInteractorApi, navigation) as T
        } else if (modelClass.isAssignableFrom(AddDefinitionViewModel::class.java)) {
            AddDefinitionViewModel(coreInteractorApi, navigation, id) as T
        } else if (modelClass.isAssignableFrom(AddSampleViewModel::class.java)) {
            AddSampleViewModel(coreInteractorApi, navigation, id) as T
        } else if (modelClass.isAssignableFrom(EditDefinitionViewModel::class.java)) {
            EditDefinitionViewModel(coreInteractorApi, navigation, id) as T
        } else {
            throw IllegalStateException("Cannot find ViewModel class")
        }
    }

    fun setId(id: Long) {
        this.id = id
    }
}