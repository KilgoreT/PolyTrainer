package me.apomazkin.feature_vocabulary_impl.viewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.feature_vocabulary_impl.WordListViewModel
import javax.inject.Inject

class VocabularyViewModelFactory @Inject constructor(
    private val dbApi: CoreDbApi
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(WordListViewModel::class.java)) {
            WordListViewModel(dbApi) as T
        } else {
            throw IllegalStateException("Cannot find ViewModel class")
        }
    }
}