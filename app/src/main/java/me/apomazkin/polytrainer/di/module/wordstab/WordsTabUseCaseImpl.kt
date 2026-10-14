package me.apomazkin.polytrainer.di.module.dictionarytab

import me.apomazkin.candidates.PhotoDictionaryForm
import me.apomazkin.candidates.PhotoFormOutcome
import me.apomazkin.candidates.PhotoLookupOutcome
import me.apomazkin.candidates.PhotoMapping
import android.content.Context
import android.net.Uri
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_network_api.CoreNetworkApi
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.polytrainer.di.module.candidates.CandidatesFormLoader
import me.apomazkin.polytrainer.di.module.wordstab.PhotoMappingStore
import me.apomazkin.polytrainer.mapper.lookupRequestOf
import me.apomazkin.polytrainer.mapper.toOutcome
import me.apomazkin.textrecognition.RecognizedText
import me.apomazkin.textrecognition.RecognizedWord
import me.apomazkin.textrecognition.TextRecognizer
import me.apomazkin.wordstab.deps.WordsTabUseCase
import me.apomazkin.wordrow.entity.TermUiItem
import me.apomazkin.wordrow.entity.toUiItem
import me.apomazkin.flags.CountryProvider
import me.apomazkin.polytrainer.di.module.dictionary.CurrentDictionaryProvider
import me.apomazkin.polytrainer.mapper.toDomain
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Inject

private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

class WordsTabUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val wordApi: CoreDbApi.WordApi,
    private val termApi: CoreDbApi.TermApi,
    private val lexemeApi: CoreDbApi.LexemeApi,
    private val groupApi: CoreDbApi.GroupApi,
    private val wordsBatchApi: CoreDbApi.WordsBatchApi,
    private val photoMappingStore: PhotoMappingStore,
    private val prefsProvider: PrefsProvider,
    private val countryProvider: CountryProvider,
    private val currentDictionaryProvider: CurrentDictionaryProvider,
    private val textRecognizer: TextRecognizer,
    private val wordsApi: CoreNetworkApi.WordsApi,
    private val appContext: Context,
) : WordsTabUseCase {

    // IS529: один запрос на сервер слов с языками текущего словаря (Ш6).
    // Исключений от сети нет: слой отдаёт один из трёх исходов; без словаря
    // сюда не попасть (кнопка скрыта), поэтому его отсутствие — ошибка программы.
    // IS532: словарь снимка уходит в исход по id (AWP-01).
    override suspend fun lookupWords(tokens: List<RecognizedWord>, runId: Long): PhotoLookupOutcome {
        val dictionary = currentDictionary()
        val request = lookupRequestOf(dictionary, tokens)
        val result = wordsApi.lookup(request, runId)
        return result.toOutcome(dictionaryId = dictionary.id)
    }

    // IS532 (Ш1): форма словаря снимка — включённые живые компоненты,
    // зависящие от лексемы, опции «Части речи», группы, леммы словаря с
    // текстами лемм ответа и запомненные соответствия. Словаря нет → В1
    // (AWP-24.4); любое исключение чтения → В1 (AWP-24.1); отмена пролетает.
    override suspend fun loadPhotoForm(dictionaryId: Long, lemmaTexts: List<String>): PhotoFormOutcome {
        val dictionary = dictionaryApi.getDictionaryById(dictionaryId) ?: return PhotoFormOutcome.DictionaryMissing
        return try {
            PhotoFormOutcome.Loaded(
                form = formLoader.formOf(dictionary, lemmaTexts),
                remembered = photoMappingStore.read(dictionaryId),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            PhotoFormOutcome.Failed(cause = "${failure.javaClass.simpleName}: ${failure.message}")
        }
    }

    // IS533: сборка формы — общая с диалогом карточки слова (CandidatesFormLoader).
    private val formLoader = CandidatesFormLoader(
        lexemeApi = lexemeApi,
        groupApi = groupApi,
        termApi = termApi,
        wordsBatchApi = wordsBatchApi,
    )

    // IS532 (Ш4): исключение записи пролетает к эффекту — это «не добавилось» (AWP-21).
    override suspend fun addPhotoWords(batch: WordsBatch): AddWordsBatchOutcome =
        wordsBatchApi.addWordsBatch(batch)

    override suspend fun savePhotoMapping(dictionaryId: Long, mapping: PhotoMapping) {
        photoMappingStore.write(dictionaryId, mapping)
    }

    private suspend fun currentDictionary(): DictionaryApiEntity =
        currentDictionaryProvider.flowCurrentDict().first()
            ?: throw IllegalStateException("Dictionary not found")

    // IS527: распознавание целиком в библиотеке. Снимок камеры (наш
    // FileProvider) после распознавания не нужен — удаляется в любом исходе;
    // картинка из галереи не наша, её не трогаем.
    override suspend fun recognizeText(uri: Uri): RecognizedText = try {
        textRecognizer.recognize(uri)
    } finally {
        if (uri.authority == appContext.packageName + FILE_PROVIDER_SUFFIX) {
            appContext.contentResolver.delete(uri, null, null)
        }
    }

    override suspend fun getCurrentDict(): DictUiEntity? {
        prefsProvider
            .getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)?.let { id ->
                dictionaryApi.getDictionaryById(id)?.let {
                    return DictUiEntity(
                        id = it.id,
                        flagRes = it.numericCode?.let { nc -> countryProvider.getFlagRes(nc) } ?: 0,
                        title = it.name,
                        numericCode = it.numericCode ?: 0,
                    )
                }
            } ?: dictionaryApi.getDictionaryList()
            .firstOrNull()
            ?.let {
                prefsProvider.setLong(
                    PrefKey.CURRENT_DICTIONARY_ID_LONG,
                    it.id
                )
                return DictUiEntity(
                    id = it.id,
                    flagRes = it.numericCode?.let { nc -> countryProvider.getFlagRes(nc) } ?: 0,
                    title = it.name,
                    numericCode = it.numericCode ?: 0,
                )
            }
        // IS476: нет ни prefs, ни словарей — null вместо throw
        return null
    }

    // IS500: резолв текущего словаря — через единый CurrentDictionaryProvider.
    override fun flowCurrentDict(): Flow<DictUiEntity?> = currentDictionaryProvider
        .flowCurrentDict()
        .map { dict ->
            // IS476: null если ни по id, ни fallback ничего не нашли — валидное состояние
            dict?.let {
                DictUiEntity(
                    id = it.id,
                    flagRes = it.numericCode?.let { nc -> countryProvider.getFlagRes(nc) } ?: 0,
                    title = it.name,
                    numericCode = it.numericCode ?: 0,
                )
            }
        }

    // TODO: Убрать нулеабельность в Dictionary: id и name
    override suspend fun addWord(value: String): Long =
        prefsProvider
            .getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
            ?.let { id ->
                dictionaryApi.getDictionaryById(id)?.let {
                    return wordApi.addWordSuspend(value, it.id.toInt())
                } ?: throw IllegalStateException("Dictionary not found")
            } ?: throw IllegalStateException("Dictionary not found")

    override suspend fun updateWord(id: Long, value: String): Boolean =
        wordApi.updateWordSuspend(id, value)

    override suspend fun deleteWord(wordId: Long) {
        wordApi.deleteWordSuspend(wordId)
    }

    override suspend fun changeDict(id: Long) {
        prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, id)
    }

    // TODO: Передавать dictionaryId сюда в параметр. нехер делать лишний запрос
    // dictionaryId хранить в стейте
    override suspend fun getWordList(): List<TermUiItem> {
        return prefsProvider
            .getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
            ?.let { id ->
                dictionaryApi.getDictionaryById(id)?.id?.toInt()?.let { dictionaryId: Int ->
                    termApi.getTermList(dictionaryId)
                        .map { term ->
                            TermUiItem(
                                id = term.word.id,
                                wordValue = term.word.value,
                                dictionaryId = term.word.dictionaryId,
                                addDate = term.word.addDate,
                                changeDate = term.word.changeDate,
                                lexemeList = term.lexemes
                                    .map { it.toDomain() }
                                    .map { it.toUiItem() }
                            )
                        }
                } ?: throw IllegalStateException("Dictionary not found")
            } ?: throw IllegalStateException("Dictionary not found")
    }

    override fun searchTerms(pattern: String, dictionaryId: Int): Flow<PagingData<TermUiItem>> {
        return termApi.searchTermsPaging(
            pattern = pattern,
            dictionaryId = dictionaryId
        ).map { pagingData ->
            pagingData.map { term ->
                TermUiItem(
                    id = term.word.id,
                    wordValue = term.word.value,
                    dictionaryId = term.word.dictionaryId,
                    addDate = term.word.addDate,
                    changeDate = term.word.changeDate,
                    lexemeList = term.lexemes
                        .map { it.toDomain() }
                        .map { it.toUiItem() }
                )
            }

        }
    }
}