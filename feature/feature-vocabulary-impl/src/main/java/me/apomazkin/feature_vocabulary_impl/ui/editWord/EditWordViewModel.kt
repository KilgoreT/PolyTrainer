package me.apomazkin.feature_vocabulary_impl.ui.editWord

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.apomazkin.core_db_api.entity.Word
import me.apomazkin.core_interactor.CoreInteractorApi
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation

class EditWordViewModel(
    private val coreInteractorApi: CoreInteractorApi,
    private val navigation: FeatureVocabularyNavigation
) : ViewModel() {

    val wordValue = MutableLiveData<String>("default")
    var wordId: Long? = null

    fun setup(id: Long, value: String) {
        wordValue.postValue(value)
        wordId = id
    }

    fun onEditWord() {
        wordValue.value?.let { value ->
            wordId?.let { id ->
                val word = Word(id, value.trim())
                coreInteractorApi
                    .updateWordUseCase()
                    .updateWord(word)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        navigation.closeDialog()
                    }
            }
        }
    }
}