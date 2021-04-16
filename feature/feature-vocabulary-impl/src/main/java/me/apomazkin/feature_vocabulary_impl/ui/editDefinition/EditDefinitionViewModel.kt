package me.apomazkin.feature_vocabulary_impl.ui.editDefinition

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.core_interactor.CoreInteractorApi
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation

@SuppressLint("CheckResult")
class EditDefinitionViewModel(
    private val coreInteractorApi: CoreInteractorApi,
    private val navigation: FeatureVocabularyNavigation,
    id: Long
) : ViewModel() {

    var definition: Definition? = null
    val definitionValue = MutableLiveData<String>()

    init {
        coreInteractorApi
            .getDefinitionUseCase()
            .getDefinition(id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                definition = it
                definitionValue.postValue(it.value)
            }, {
                //
            })
    }

    fun onEditDefinition() {
        definitionValue.value?.let { newValue ->
            val result = definition?.copy(value = newValue.trim())
            result?.let {
                coreInteractorApi
                    .updateDefinitionUseCase()
                    .updateDefinition(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        navigation.closeDialog()
                    }
            }
        }
    }

}