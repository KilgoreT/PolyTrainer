package me.apomazkin.feature_vocabulary_impl.ui.addDefinition

import android.view.View
import android.widget.AdapterView
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.chip.ChipGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.apomazkin.core_db_api.entity.*
import me.apomazkin.core_interactor.CoreInteractorApi
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation
import me.apomazkin.feature_vocabulary_impl.R

class AddDefinitionViewModel(
    private val coreInteractorApi: CoreInteractorApi,
    private val navigation: FeatureVocabularyNavigation,
    private val id: Long
) : ViewModel(), AdapterView.OnItemSelectedListener, ChipGroup.OnCheckedChangeListener {

    val addingDefinition = MutableLiveData<String>()
    val grade = MutableLiveData<Grade>(null)
    val nounOptionsVisibility = ObservableField<Int>(View.GONE)
    val nounCountability = MutableLiveData<Noun.Countability>(null)
    val verbOptionsVisibility = ObservableField<Int>(View.GONE)
    val verbTransitivity = MutableLiveData<Verb.Transitivity>(null)
    val adjectiveOptionsVisibility = ObservableField<Int>(View.GONE)
    val adjOrder = MutableLiveData<Adjective.Order>(null)
    val adjGradability = MutableLiveData<Adjective.Gradability>(null)


    // TODO: 26.10.2020 заменить стрингу на sealed class.
    //  1. Для этого добавить в sealed дефолтные значения undefined.
    //  2. Потом добавить в UI появление этих опций частей речи,
    //  чтобы по выбору обновлось знанчение [me.apomazkin.core_db_api.entity.WordClass] в данной LiveData
    private val wordClass = MutableLiveData<String?>()

    fun onNounSelect() {
        wordClass.postValue("noun")
        nounOptionsVisibility.set(View.VISIBLE)
        verbOptionsVisibility.set(View.GONE)
        adjectiveOptionsVisibility.set(View.GONE)
    }

    fun onVerbSelect() {
        wordClass.postValue("verb")
        verbOptionsVisibility.set(View.VISIBLE)
        nounOptionsVisibility.set(View.GONE)
        adjectiveOptionsVisibility.set(View.GONE)
    }

    fun onAdjectiveSelect() {
        wordClass.postValue("adjective")
        adjectiveOptionsVisibility.set(View.VISIBLE)
        nounOptionsVisibility.set(View.GONE)
        verbOptionsVisibility.set(View.GONE)
    }

    fun onAdverbSelect() {
        wordClass.postValue("adverb")
        nounOptionsVisibility.set(View.GONE)
        verbOptionsVisibility.set(View.GONE)
        adjectiveOptionsVisibility.set(View.GONE)
    }

    fun onAddDefinition() {
        addingDefinition.value?.let {
            coreInteractorApi
                .addDefinitionUseCase()
                .addDefinition(
                    Definition(
                        wordId = id,
                        value = it,
                        wordClass = when (wordClass.value) {
                            "noun" -> {
                                Noun(
                                    countability = nounCountability.value,
                                    grade = grade.value,
                                )
                            }
                            "verb" -> {
                                Verb(
                                    transitivity = verbTransitivity.value,
                                    grade = grade.value,
                                )
                            }
                            "adjective" -> Adjective(
                                order = adjOrder.value,
                                gradability = adjGradability.value,
                                grade = grade.value,
                            )
                            "adverb" -> Adverb(
                                grade = grade.value,
                            )
                            else -> null
                        }
                    )
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {}
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
        when (group?.id ?: 0) {
            R.id.chipGroupGrade -> {
                setupGrade(checkedId)
            }
            R.id.chipGroupNoun -> {
                setupNounOption(checkedId)
            }
            R.id.chipGroupVerb -> {
                setupVerbOption(checkedId)
            }
            R.id.chipGroupAdjOrder -> {
                setupAdjOrder(checkedId)
            }
            R.id.chipGroupAdjGradability -> {
                setupAdjGradability(checkedId)
            }
        }

    }

    private fun setupGrade(checkedId: Int) {
        when (checkedId) {
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

    private fun setupNounOption(checkedId: Int) {
        nounCountability.value = when (checkedId) {
            R.id.chipNounCountable -> {
                Noun.Countability.COUNTABLE
            }
            R.id.chipNounUncountable -> {
                Noun.Countability.UNCOUNTABLE
            }
            R.id.chipNounPlural -> {
                Noun.Countability.PLURAL
            }
            R.id.chipNounUsuallyPlural -> {
                Noun.Countability.USUALLY_PLURAL
            }
            R.id.chipNounUsuallySingular -> {
                Noun.Countability.USUALLY_SINGULAR
            }
            else -> null
        }
    }

    private fun setupVerbOption(checkedId: Int) {
        verbTransitivity.value = when (checkedId) {
            R.id.chipVerbTransitive -> {
                Verb.Transitivity.TRANSITIVE
            }
            R.id.chipVerbIntransitive -> {
                Verb.Transitivity.INTRANSITIVE
            }
            else -> null
        }
    }

    private fun setupAdjOrder(checkedId: Int) {
        adjOrder.value = when (checkedId) {
            R.id.chipAdjOrderAfterNoun -> {
                Adjective.Order.AFTER_NOUN
            }
            R.id.chipAdjOrderAfterVerb -> {
                Adjective.Order.AFTER_VERB
            }
            R.id.chipAdjOrderBeforeNoun -> {
                Adjective.Order.BEFORE_NOUN
            }
            else -> null
        }
    }

    private fun setupAdjGradability(checkedId: Int) {
        adjGradability.value = when (checkedId) {
            R.id.chipAdjGradabilityComparative -> {
                Adjective.Gradability.COMPARATIVE
            }
            R.id.chipAdjGradabilitySuperlative -> {
                Adjective.Gradability.SUPERLATIVE
            }
            R.id.chipAdjGradabilityNotGradable -> {
                Adjective.Gradability.NOT_GRADABLE
            }
            else -> null
        }
    }
}