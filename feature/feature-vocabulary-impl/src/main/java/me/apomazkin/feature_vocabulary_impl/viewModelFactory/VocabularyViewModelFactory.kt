package me.apomazkin.feature_vocabulary_impl.viewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_interactor.CoreInteractorApi
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation
import me.apomazkin.feature_vocabulary_impl.WordListViewModel
import me.apomazkin.feature_vocabulary_impl.loadState.LoadState
import me.apomazkin.feature_vocabulary_impl.ui.addDefinition.AddDefinitionViewModel
import me.apomazkin.feature_vocabulary_impl.ui.addWord.AddWordViewModel
import javax.inject.Inject

class VocabularyViewModelFactory @Inject constructor(
    private val coreInteractorApi: CoreInteractorApi,
    private val dbApi: CoreDbApi,
    private val navigation: FeatureVocabularyNavigation,
    private val delegate: LoadState
) : ViewModelProvider.Factory {

    private var id: Long = -1

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(WordListViewModel::class.java)) {
            WordListViewModel(coreInteractorApi, navigation, delegate) as T
        } else if (modelClass.isAssignableFrom(AddWordViewModel::class.java)) {
            AddWordViewModel(dbApi, navigation) as T
        } else if (modelClass.isAssignableFrom(AddDefinitionViewModel::class.java)) {
            AddDefinitionViewModel(dbApi, navigation, id) as T
        } else {
            throw IllegalStateException("Cannot find ViewModel class")
        }
    }

    fun setId(id: Long) {
        this.id = id
    }
}