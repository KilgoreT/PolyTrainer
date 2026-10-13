package me.apomazkin.polytrainer.di.module.dictionarytab

import android.content.Context
import android.net.Uri
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_network_api.CoreNetworkApi
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.lexeme.lexemeTextKey
import me.apomazkin.lexeme.wordTextKey
import me.apomazkin.polytrainer.di.module.wordstab.PhotoMappingStore
import me.apomazkin.polytrainer.mapper.lookupRequestOf
import me.apomazkin.polytrainer.mapper.toOutcome
import me.apomazkin.textrecognition.RecognizedText
import me.apomazkin.textrecognition.RecognizedWord
import me.apomazkin.textrecognition.TextRecognizer
import me.apomazkin.wordstab.deps.PhotoComponent
import me.apomazkin.wordstab.deps.PhotoDictionaryForm
import me.apomazkin.wordstab.deps.PhotoExistingLemma
import me.apomazkin.wordstab.deps.PhotoFormOutcome
import me.apomazkin.wordstab.deps.PhotoGroup
import me.apomazkin.wordstab.deps.PhotoLookupOutcome
import me.apomazkin.wordstab.deps.PhotoMapping
import me.apomazkin.wordstab.deps.PhotoPartOfSpeechOption
import me.apomazkin.wordstab.deps.PhotoTemplate
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

/** Контракт `getTermsByIds`: не больше 50 id за вызов (bind-лимит SQLite). */
private const val TERMS_CHUNK = 50

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
                form = formOf(dictionary, lemmaTexts),
                remembered = photoMappingStore.read(dictionaryId),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            PhotoFormOutcome.Failed(cause = "${failure.javaClass.simpleName}: ${failure.message}")
        }
    }

    private suspend fun formOf(dictionary: DictionaryApiEntity, lemmaTexts: List<String>): PhotoDictionaryForm {
        val types = lexemeApi.getComponentTypes(dictionary.id).filter { it.enabled && it.dependsOn == DependencyTarget.Lexeme }
        val partOfSpeech = types.firstOrNull { it.systemKey == BuiltInComponent.PART_OF_SPEECH }
        val options = partOfSpeech?.let { lexemeApi.getComponentOptions(it.id) }.orEmpty()
        val groups = groupApi.groupTree(dictionary.id).first()
        return PhotoDictionaryForm(
            dictionaryId = dictionary.id,
            translationLanguage = dictionary.translationLanguage,
            components = types.map { it.toPhotoComponent() },
            partOfSpeechOptions = options
                .filter { it.removedAt == null }
                .map { PhotoPartOfSpeechOption(id = it.id, key = it.systemKey, label = it.label, position = it.position) },
            groups = groups.map { PhotoGroup(id = it.id, name = it.name) },
            existingLemmas = existingLemmasOf(dictionary.id, lemmaTexts, types),
        )
    }

    private fun ComponentTypeApiEntity.toPhotoComponent(): PhotoComponent = PhotoComponent(
        id = id,
        systemKey = systemKey,
        name = name,
        template = when (template) {
            ComponentTemplate.TEXT -> PhotoTemplate.TEXT
            ComponentTemplate.CHOICE -> PhotoTemplate.CHOICE
            ComponentTemplate.CAPTIONED_TEXT -> PhotoTemplate.CAPTIONED_TEXT
            ComponentTemplate.IMAGE -> PhotoTemplate.IMAGE
        },
        isCore = core,
        isMultiple = isMultiple,
        position = position,
    )

    // AWP-07, AWP-18: леммы словаря с тем же текстом (без регистра, краёв и
    // формы Unicode) — лёгким запросом текстов, затем контент пачками по 50;
    // порядок — самая новая первой (id DESC, порядок списка «Слова»).
    private suspend fun existingLemmasOf(
        dictionaryId: Long,
        lemmaTexts: List<String>,
        types: List<ComponentTypeApiEntity>,
    ): List<PhotoExistingLemma> {
        val keys = lemmaTexts.mapTo(HashSet()) { wordTextKey(it) }
        val ids = wordsBatchApi.getWordTexts(dictionaryId).filter { wordTextKey(it.value) in keys }.map { it.id }
        val terms = ids.chunked(TERMS_CHUNK).flatMap { termApi.getTermsByIds(it) }.sortedByDescending { it.word.id }
        val coreTypeId = firstTextCoreId(types)
        return terms.map { term ->
            PhotoExistingLemma(
                wordId = term.word.id,
                text = term.word.value,
                lexemeCount = term.lexemes.size,
                coreValues = term.coreValues(coreTypeId),
                lexemeTextKeys = term.textKeys(),
                groupNames = groupApi.wordGroups(term.word.id).first().map { it.name },
            )
        }
    }

    /** Первое включённое ядро шаблона «текст»: встроенные раньше своих, затем position, затем id (AWP-06, AWP-18.7). */
    private fun firstTextCoreId(types: List<ComponentTypeApiEntity>): Long? = types
        .filter { it.template == ComponentTemplate.TEXT && it.core }
        .sortedWith(compareBy({ it.systemKey == null }, { it.position }, { it.id }))
        .firstOrNull()
        ?.id

    private fun TermApiEntity.coreValues(coreTypeId: Long?): List<String> = lexemes.mapNotNull { lexeme ->
        lexeme.components
            .firstOrNull { it.type.id == coreTypeId && it.removedAt == null }
            ?.data
            ?.let { it as? TextValues }
            ?.value
            ?.value
    }

    private fun TermApiEntity.textKeys(): Set<String> = lexemes
        .flatMap { lexeme -> lexeme.components }
        .filter { it.removedAt == null }
        .mapNotNull { (it.data as? TextValues)?.value?.value }
        .mapTo(HashSet()) { lexemeTextKey(it) }

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