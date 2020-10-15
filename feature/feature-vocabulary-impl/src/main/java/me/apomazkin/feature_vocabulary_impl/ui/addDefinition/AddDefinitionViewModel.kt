package me.apomazkin.feature_vocabulary_impl.ui.addDefinition

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.core_db_api.entity.Noun
import me.apomazkin.core_db_api.entity.Verb
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation

class AddDefinitionViewModel(
    private val dbApi: CoreDbApi,
    private val navigation: FeatureVocabularyNavigation,
    private val id: Long
) : ViewModel() {

    val addingDefinition = MutableLiveData<String>()
    private val partOfSpeech = MutableLiveData<String?>()

    fun onVerbSelect() {
        partOfSpeech.postValue("verb")
    }

    fun onNounSelect() {
        partOfSpeech.postValue("noun")
    }

    fun onAddDefinition() {
        addingDefinition.value?.let {
            dbApi.addDefinition(
                Definition(
                    wordId = id,
                    definition = it,
                    partOfSpeech = when (partOfSpeech.value) {
                        "noun" -> Noun(null)
                        "verb" -> Verb(null)
                        else -> null
                    }
                )
            )
        }
        navigation.closeDialog()
    }
}