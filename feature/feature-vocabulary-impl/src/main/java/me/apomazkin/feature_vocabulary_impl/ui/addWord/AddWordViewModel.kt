package me.apomazkin.feature_vocabulary_impl.ui.addWord

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation

class AddWordViewModel(
    private val dbApi: CoreDbApi,
    private val navigation: FeatureVocabularyNavigation
) : ViewModel() {

    val addingWord = MutableLiveData<String>()

    fun onAddWord() {
        addingWord.value?.let {
            dbApi.addWord(it)
        }
        navigation.closeDialog()
    }
}