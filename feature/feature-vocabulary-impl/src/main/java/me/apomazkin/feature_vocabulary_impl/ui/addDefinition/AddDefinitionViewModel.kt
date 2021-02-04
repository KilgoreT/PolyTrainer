package me.apomazkin.feature_vocabulary_impl.ui.addDefinition

import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.chip.ChipGroup
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.*
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation
import me.apomazkin.feature_vocabulary_impl.R

class AddDefinitionViewModel(
    private val dbApi: CoreDbApi,
    private val navigation: FeatureVocabularyNavigation,
    private val id: Long
) : ViewModel(), AdapterView.OnItemSelectedListener, ChipGroup.OnCheckedChangeListener {

    val addingDefinition = MutableLiveData<String>()
    val grade = MutableLiveData<Grade>(null)

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
//        nounCountableVisibility.set(View.VISIBLE)
//        verbTransitiveVisibility.set(View.GONE)
//        nounCountableVisibility.postValue(View.VISIBLE)
    }

    fun onVerbSelect() {
        wordClass.postValue("verb")
//        nounCountableVisibility.set(View.GONE)
//        verbTransitiveVisibility.set(View.VISIBLE)
//        nounCountableVisibility.postValue(View.GONE)
    }

    fun onAdjectiveSelect() {
        wordClass.postValue("adjective")
//        nounCountableVisibility.set(View.GONE)
//        verbTransitiveVisibility.set(View.GONE)
//        nounCountableVisibility.postValue(View.GONE)
    }

    fun onAdverbSelect() {
        wordClass.postValue("adverb")
//        nounCountableVisibility.set(View.GONE)
//        verbTransitiveVisibility.set(View.GONE)
//        nounCountableVisibility.postValue(View.GONE)
    }

    fun onAddDefinition() {
        Log.d("###", "noun: ${nounCountableChecked.value}")
        Log.d("###", "verb: ${verbTransitiveChecked.value}")
        Log.d("###", "grade: ${grade.value}")
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
                            Noun(
                                grade = grade.value,
                                countability = countability
                            )
                        }
                        "verb" -> {
                            Verb(
                                grade = grade.value,
                            )
                        }
                        "adjective" -> Adjective(
                            grade = grade.value,
                        )
                        "adverb" -> Adverb(
                            grade = grade.value,
                        )
                        else -> null
                    }
                )
            )
        }
        navigation.closeDialog()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (position) {
            0 -> {
                onNounSelect()
            }
            1 -> {
                onVerbSelect()
            }
            2 -> {
                onAdjectiveSelect()
            }
            3 -> {
                onAdverbSelect()
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onCheckedChanged(group: ChipGroup?, checkedId: Int) {
        when (checkedId) {
            R.id.chipNone -> {
                grade.value = null
            }
            R.id.chipA1 -> {
                grade.value = Grade.A1
            }
            R.id.chipA2 -> {
                grade.value = Grade.A2
            }
            R.id.chipB1 -> {
                grade.value = Grade.B1
            }
            R.id.chipB2 -> {
                grade.value = Grade.B2
            }
            R.id.chipC1 -> {
                grade.value = Grade.C1
            }
            R.id.chipC2 -> {
                grade.value = Grade.C2
            }
            else -> {
                grade.value = null
            }
        }
    }
}