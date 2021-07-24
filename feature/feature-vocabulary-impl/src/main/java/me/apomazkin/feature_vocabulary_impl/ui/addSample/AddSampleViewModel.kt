package me.apomazkin.feature_vocabulary_impl.ui.addSample

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.apomazkin.core_interactor.CoreInteractorApi
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation

class AddSampleViewModel(
    private val coreInteractorApi: CoreInteractorApi,
    private val navigation: FeatureVocabularyNavigation,
    private val id: Long
) : ViewModel() {

    val sampleValue = MutableLiveData<String>()
    val sampleSource = MutableLiveData<String>()

    fun onAddSample() {
        sampleValue.value?.let {
            coreInteractorApi
                .addSampleUseCase()
                .addSample(
                    definitionId = id,
                    value = it,
                    source = sampleSource.value
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    navigation.closeDialog()
                }
        }
    }

}