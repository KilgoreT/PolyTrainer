package me.apomazkin.feature_vocabulary_impl.ui.addDefinition

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation

class AddDefinitionViewModel(
    private val dbApi: CoreDbApi,
    private val navigation: FeatureVocabularyNavigation,
    private val id: Long
) : ViewModel() {

    val addingDefinition = MutableLiveData<String>()

    fun onAddDefinition() {
        addingDefinition.value?.let {
            dbApi.addDefinition(
                Definition(
                    wordId = id,
                    definition = it
                )
            )
        }
        navigation.closeDialog()
    }
}