package me.apomazkin.feature_vocabulary_impl.ui.addDefinition

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.*
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation

class AddDefinitionViewModel(
    private val dbApi: CoreDbApi,
    private val navigation: FeatureVocabularyNavigation,
    private val id: Long
) : ViewModel() {

    val addingDefinition = MutableLiveData<String>()

    // TODO: 26.10.2020 заменить стрингу на sealed class.
    //  1. Для этого добавить в sealed дефолтные значения undefined.
    //  2. Потом добавить в UI появление этих опций частей речи,
    //  чтобы по выбору обновлось знанчение me.apomazkin.core_db_api.entity.PartOfSpeech в данной LiveData
    private val partOfSpeech = MutableLiveData<String?>()

    fun onNounSelect() {
        partOfSpeech.postValue("noun")
    }

    fun onVerbSelect() {
        partOfSpeech.postValue("verb")
    }

    fun onAdjectiveSelect() {
        partOfSpeech.postValue("adjective")
    }

    fun onAdverbSelect() {
        partOfSpeech.postValue("adverb")
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
                        "adjective" -> Adjective
                        "adverb" -> Adverb
                        else -> null
                    }
                )
            )
        }
        navigation.closeDialog()
    }
}