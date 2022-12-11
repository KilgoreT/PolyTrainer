package me.apomazkin.feature_vocabulary_impl.ui.wordList

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import me.apomazkin.core_db_api.entity.*
import me.apomazkin.core_interactor.CoreInteractorApi
import me.apomazkin.core_interactor.LangGod
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation
import me.apomazkin.feature_vocabulary_impl.loadState.LoadState
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@SuppressLint("CheckResult")
class WordListViewModel @Inject constructor(
    private val coreInteractorApi: CoreInteractorApi,
    private val navigation: FeatureVocabularyNavigation,
    private val loadStateDelegate: LoadState
) : ViewModel(), LoadState by loadStateDelegate {

    val data = MutableLiveData<List<Term>>()
    val wordPattern = MutableLiveData<String>("")
    val transition = MutableLiveData<String>()
    val argh = MutableLiveData<String>()

    init {
        loadData()
    }

    @SuppressLint("CheckResult")
    fun loadData() {
        wordPattern.observeForever {
            coreInteractorApi
                .searchTermUseCase()
                .getTermList("%$it%")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { result ->
                        if (result.isEmpty()) onEmpty() else onData()
                        data.postValue(result)
                    },
                    { onError(it) }
                )
        }
    }

    fun addWord() {
        wordPattern.value?.let {
            if (it.isNotBlank()) {
                coreInteractorApi
                    .addWordUseCase()
                    .addWord(it.trim())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        wordPattern.postValue("")
                    }
            }
        }
        transition.postValue("START")

    }

    @SuppressLint("CheckResult")
    fun removeWord(id: Long) {
        coreInteractorApi
            .removeWordUseCase()
            .removeWord(id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                // TODO: 02.12.2020 реализовать обратную связь
            }, { error ->
                // TODO: 02.12.2020 реакция на ошибку
            })
    }

    fun addDefinition(id: Long?) {
        id?.let {
            navigation.addDefinitionDialog(it)
        }
    }

    fun editDefinition(id: Long) {
        navigation.editDefinitionDialog(id)
    }

    fun deleteDefinition(id: Long?) {
        id?.let {
            coreInteractorApi
                .removeDefinitionUseCase()
                .removeDefinition(it)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    // TODO: 02.12.2020 реализовать обратную связь
                }, { error ->
                    // TODO: 02.12.2020 реакция на ошибку
                })
            coreInteractorApi.removeWriteQuizUseCase()
                .removeWriteQuiz(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    // TODO: 02.12.2020 реализовать обратную связь
                }, { error ->
                    // TODO: 02.12.2020 реакция на ошибку
                })

        }
    }

    fun editWord(word: Word) {
        word.id?.let { id ->
            word.value?.let { value ->
                navigation.editWordDialog(id, value.trim())
            }
        }
    }

    fun addSample(definitionId: Long) {
        navigation.addSampleDialog(definitionId)
    }

    @SuppressLint("CheckResult")
    fun argh() {
        coreInteractorApi
            .getDefinitionUseCase()
            .getDefinition()
            .zipWith(
                coreInteractorApi
                    .getWriteQuizByAccessTimeUseCase()
                    .getWriteQuizList(LangGod.langId), BiFunction { defList, quizList ->
                    val result = mutableListOf<WriteQuiz>()
                    quizList.forEach { quiz ->
                        val temp = defList.filter { item -> item.id == quiz.definition.id }
                        if (temp.isEmpty()) {
                            result.add(quiz)
                        }
                    }
                    return@BiFunction result
                })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { ttt ->
                argh.postValue("Wrong: ${ttt.size}")
            }
    }

    @SuppressLint("CheckResult")
    fun arghDelete() {
        coreInteractorApi
            .getDefinitionUseCase()
            .getDefinition()
            .zipWith(coreInteractorApi
                .getWriteQuizByAccessTimeUseCase()
                .getWriteQuizList(LangGod.langId), BiFunction { defList, quizList ->
                val result = mutableListOf<WriteQuiz>()
                quizList.forEach { quiz ->
                    val temp = defList.filter { item -> item.id == quiz.definition.id }
                    if (temp.isEmpty()) {
                        result.add(quiz)
                    }
                }
                return@BiFunction result
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { ttt ->
                ttt.forEach { item ->
                    coreInteractorApi
                        .removeWriteQuizUseCase()
                        .removeWriteQuiz(item.definition.id ?: throw IllegalStateException())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                        }
                }
            }
    }

    fun backUpAll(googleSignInAccount: GoogleAccountCredential) {
        viewModelScope.launch {
            coreInteractorApi
                .getDumpUseCase()
                .getDump()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { dump ->
                    backDump(googleSignInAccount, dump)
                }
        }
    }

    private fun backDump(googleSignInAccount: GoogleAccountCredential, dump: Dump) {
        viewModelScope.launch {

            val time = Calendar.getInstance().time
            val df = SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.getDefault())
            val backFileName = df.format(time)

            val driveProvider = DriveProvider(googleSignInAccount)
            val sheetsProvider = SheetsProvider(googleSignInAccount)

            driveProvider.checkAppRoot()

            sheetsProvider.createSpreadsheet(backFileName, dump)
            driveProvider.moveFileFromRootToAppFolder(backFileName)
        }
    }

    fun restoreAll(accountCredential: GoogleAccountCredential) {
        viewModelScope.launch {
            val driveProvider = DriveProvider(accountCredential)
            val sheetsProvider = SheetsProvider(accountCredential)
            driveProvider.getSheetFiles().maxByOrNull { it.modifiedTime.value }
                ?.id
                ?.also { fileId ->
                    Log.d("###", "WordListViewModel / 230 / restoreAll: $fileId")
                    sheetsProvider.getDataFromSheet(fileId).also { dump ->
                        Log.d("###", "WordListViewModel / 233 / restoreAll: $dump")
                        coreInteractorApi.getDumpUseCase().restore(dump)
                    }
                }
        }
    }

    fun checkLang() {
        coreInteractorApi
            .getLanguageUseCase()
            .getAllLanguage()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    Log.d("###", "lang count: ${result.size}")
                    verifyLang(result)
                },
                { onError(it) }
            )
    }

    private fun verifyLang(result: List<Language>) {
        result.firstOrNull { it.code == "en" }
            ?: let {
                Log.d("###", "verifyLang: En non exist")
                coreInteractorApi
                    .addLanguageUseCase()
                    .addLanguage(
                        code = "en",
                        name = "English"
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        Log.d("###", "verifyLang: End Added!")
                    }
            }

        result.firstOrNull { it.code == "fr" } ?: let {
            Log.d("###", "verifyLang: Fr non exist")
            coreInteractorApi
                .addLanguageUseCase()
                .addLanguage(
                    code = "fr",
                    name = "French"
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Log.d("###", "verifyLang: Fr Added")
                }
        }
    }

}