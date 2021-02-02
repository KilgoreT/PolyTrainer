package me.apomazkin.feature_vocabulary_impl.ui.addDefinition

import android.util.Log
import android.view.View
import androidx.databinding.ObservableField
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
    val nounCountableVisibility1 = MutableLiveData<Int>()
    val nounCountableVisibility = ObservableField<Int>(View.GONE)
    val verbTransitiveVisibility = ObservableField<Int>(View.GONE)
    val nounCountableChecked = MutableLiveData<Boolean>(false)
    val verbTransitiveChecked = MutableLiveData<Boolean>(false)


    // TODO: 26.10.2020 заменить стрингу на sealed class.
    //  1. Для этого добавить в sealed дефолтные значения undefined.
    //  2. Потом добавить в UI появление этих опций частей речи,
    //  чтобы по выбору обновлось знанчение [me.apomazkin.core_db_api.entity.WordClass] в данной LiveData
    private val wordClass = MutableLiveData<String?>()

    fun onNounSelect() {
        wordClass.postValue("noun")
        nounCountableVisibility.set(View.VISIBLE)
        verbTransitiveVisibility.set(View.GONE)
//        nounCountableVisibility.postValue(View.VISIBLE)
    }

    fun onVerbSelect() {
        wordClass.postValue("verb")
        nounCountableVisibility.set(View.GONE)
        verbTransitiveVisibility.set(View.VISIBLE)
//        nounCountableVisibility.postValue(View.GONE)
    }

    fun onAdjectiveSelect() {
        wordClass.postValue("adjective")
        nounCountableVisibility.set(View.GONE)
        verbTransitiveVisibility.set(View.GONE)
//        nounCountableVisibility.postValue(View.GONE)
    }

    fun onAdverbSelect() {
        wordClass.postValue("adverb")
        nounCountableVisibility.set(View.GONE)
        verbTransitiveVisibility.set(View.GONE)
//        nounCountableVisibility.postValue(View.GONE)
    }

    fun onAddDefinition() {
        Log.d("###", "noun: ${nounCountableChecked.value}")
        Log.d("###", "verb: ${verbTransitiveChecked.value}")
        addingDefinition.value?.let {
            dbApi.addDefinition(
                Definition(
                    wordId = id,
                    definition = it,
                    wordClass = when (wordClass.value) {
                        "noun" -> {
                            val countability =
                                if (nounCountableChecked.value == true) Noun.Countability.COUNTABLE
                                else null
                            Noun(countability)
                        }
                        "verb" -> {
                            val options =
                                if (verbTransitiveChecked.value == true) (0L or OPT_TRANSITIVE) else 0L
                            Verb(null)
                        }
                        "adjective" -> Adjective()
                        "adverb" -> Adverb()
                        else -> null
                    }
                )
            )
        }
        navigation.closeDialog()
    }
}