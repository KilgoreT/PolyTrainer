package me.apomazkin.polytrainer.di.module.wordCard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.candidates.PhotoFormOutcome
import me.apomazkin.candidates.PhotoLookupOutcome
import me.apomazkin.candidates.PhotoMapping
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_network_api.CoreNetworkApi
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.polytrainer.di.module.candidates.CandidatesFormLoader
import me.apomazkin.polytrainer.di.module.candidates.ServerAccess
import me.apomazkin.polytrainer.di.module.wordstab.PhotoMappingStore
import me.apomazkin.polytrainer.mapper.lookupRequestOf
import me.apomazkin.polytrainer.mapper.toOutcome
import me.apomazkin.textrecognition.RecognizedWord
import kotlinx.coroutines.CancellationException
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.flags.CountryProvider
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.toRef
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.polytrainer.mapper.toDomain
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.wordcard.LogTags
import me.apomazkin.wordcard.deps.AddComponentValueResult
import me.apomazkin.wordcard.deps.AvailableComponents
import me.apomazkin.wordcard.deps.RemoveComponentResult
import me.apomazkin.wordcard.deps.RemoveLexemeResult
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordcard.entity.Term
import me.apomazkin.wordcard.entity.Word
import me.apomazkin.wordcard.entity.WordId
import javax.inject.Inject

/**
 * IS481 generic UseCase impl (AGG-12). dictionaryId приходит параметром из reducer
 * (через Effect), не резолвится через prefs (A1). Перевод/определение — обычные компоненты.
 */
class WordCardUseCaseImpl @Inject constructor(
    private val wordApi: CoreDbApi.WordApi,
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val termApi: CoreDbApi.TermApi,
    private val lexemeApi: CoreDbApi.LexemeApi,
    // IS493 Э5: группы слова (пикер + чипы).
    private val groupApi: CoreDbApi.GroupApi,
    private val prefsProvider: PrefsProvider,
    private val logger: LexemeLogger,
    private val countryProvider: CountryProvider,
    // IS533: значения из словаря — сервер слов, запись пачкой, память соответствий, ключ в сборке.
    private val wordsApi: CoreNetworkApi.WordsApi,
    private val wordsBatchApi: CoreDbApi.WordsBatchApi,
    private val photoMappingStore: PhotoMappingStore,
    private val serverAccess: ServerAccess,
) : WordCardUseCase {

    // IS533: сборка формы — общая с окном снимка.
    private val formLoader = CandidatesFormLoader(
        lexemeApi = lexemeApi,
        groupApi = groupApi,
        termApi = termApi,
        wordsBatchApi = wordsBatchApi,
    )

    override suspend fun hasServerAccess(): Boolean = serverAccess.hasKey

    // WCL-03: один токен — текст слова без краёв, языки — словаря слова; null —
    // слова или словаря уже нет (WCL-01.3). Пустой текст на сервер не уходит (WCL-03.4).
    override suspend fun lookupWord(wordId: Long, runId: Long): PhotoLookupOutcome? {
        val term = termApi.getTermById(wordId) ?: return null
        val dictionary = dictionaryApi.getDictionaryById(term.word.dictionaryId) ?: return null
        val text = term.word.value.trim()
        if (text.isEmpty()) return PhotoLookupOutcome.Words(dictionaryId = dictionary.id, words = emptyList())
        val request = lookupRequestOf(dictionary, listOf(RecognizedWord(text = text, confidence = 1f, language = null)))
        return wordsApi.lookup(request, runId).toOutcome(dictionaryId = dictionary.id)
    }

    // WCL-06: форма словаря слова с лексемами только этого слова; словаря нет →
    // В1; чтение упало → В1; отмена пролетает.
    override suspend fun loadCandidatesForm(
        wordId: Long,
        lemmaTexts: List<String>,
    ): PhotoFormOutcome {
        val term = termApi.getTermById(wordId) ?: return PhotoFormOutcome.Failed(cause = "word $wordId missing")
        val dictionary = dictionaryApi.getDictionaryById(term.word.dictionaryId) ?: return PhotoFormOutcome.DictionaryMissing
        return try {
            PhotoFormOutcome.Loaded(
                form = formLoader.formForWord(dictionary, wordId, lemmaTexts),
                remembered = photoMappingStore.read(dictionary.id),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            PhotoFormOutcome.Failed(cause = "${failure.javaClass.simpleName}: ${failure.message}")
        }
    }

    // WCL-10: исключение записи пролетает к эффекту — это «не добавилось» (WCL-13).
    override suspend fun addCandidates(batch: WordsBatch): AddWordsBatchOutcome = wordsBatchApi.addWordsBatch(batch)

    override suspend fun saveCandidatesMapping(dictionaryId: Long, mapping: PhotoMapping) {
        photoMappingStore.write(dictionaryId, mapping)
    }

    override suspend fun getTermById(wordId: Long): Term? =
        termApi.getTermById(wordId)?.toDomainEntity()?.let { term ->
            val flagRes = dictionaryApi.getDictionaryById(term.dictionaryId)
                ?.numericCode
                ?.let { countryProvider.getFlagRes(it) }
            term.copy(dictionaryFlagRes = flagRes)
        }

    override suspend fun deleteWord(wordId: Long): Int = wordApi.deleteWordSuspend(wordId)

    override suspend fun updateWord(wordId: Long, value: String): Boolean =
        wordApi.updateWordSuspend(wordId, value.trim())

    override suspend fun deleteLexeme(wordId: Long, lexemeId: Long): RemoveLexemeResult? = try {
        val snapshot = lexemeApi.getLexemeById(lexemeId)?.toDomain() ?: return null
        lexemeApi.deleteLexeme(lexemeId)
        RemoveLexemeResult.Removed(snapshot)
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "deleteLexeme failed: ${e.message}")
        null
    }

    override suspend fun addLexemeWithComponent(
        wordId: Long,
        dictionaryId: Long,
        ref: ComponentTypeRef,
        data: TemplateValues,
    ): Lexeme? = try {
        lexemeApi.addLexemeWithComponents(wordId, dictionaryId, listOf(ref to data.trimmed()))
            ?.let { id -> lexemeApi.getLexemeById(id)?.toDomain() }
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "addLexemeWithComponent failed: ${e.message}")
        null
    }

    override suspend fun addComponentValue(
        lexemeId: Long,
        componentTypeId: ComponentTypeId,
        data: TemplateValues,
    ): AddComponentValueResult? = try {
        val newId = lexemeApi.addComponentValue(lexemeId, componentTypeId.id, data.trimmed())
        val lexeme = lexemeApi.getLexemeById(lexemeId)?.toDomain() ?: return null
        AddComponentValueResult(lexeme = lexeme, newComponentValueId = ComponentValueId(newId))
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "addComponentValue failed: ${e.message}")
        null
    }

    override suspend fun updateComponentValue(
        componentValueId: ComponentValueId,
        lexemeId: Long,
        data: TemplateValues,
    ): Lexeme? = try {
        val updated = lexemeApi.updateComponentValue(componentValueId.id, data.trimmed())
        if (updated <= 0) null else lexemeApi.getLexemeById(lexemeId)?.toDomain()
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "updateComponentValue failed: ${e.message}")
        null
    }

    override suspend fun deleteComponentValue(
        componentValueId: ComponentValueId,
        lexemeId: Long,
    ): RemoveComponentResult? = try {
        // IS486 фаза 3 (spec §9.1, В4): лексема живёт всегда — потеря последнего
        // значения деградирует её в черновик, каскадное удаление лексемы упразднено.
        lexemeApi.deleteComponentValue(componentValueId.id)
        lexemeApi.getLexemeById(lexemeId)?.toDomain()
            ?.let { RemoveComponentResult.ComponentRemoved(it) }
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "deleteComponentValue failed: ${e.message}")
        null
    }

    override suspend fun restoreLexemeWithComponents(
        wordId: Long,
        dictionaryId: Long,
        snapshot: Lexeme,
    ): Lexeme? = try {
        val components = snapshot.components.map { it.type.toRef() to it.data }
        lexemeApi.addLexemeWithComponents(wordId, dictionaryId, components)
            ?.let { id -> lexemeApi.getLexemeById(id)?.toDomain() }
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "restoreLexemeWithComponents failed: ${e.message}")
        null
    }

    // IS486 фаза 2: времянка-фильтр CHOICE снят — карточка получила пикер значений.
    // Девайс-баг 2026-07-21: опции идут единым реактивным снапшотом
    // (flowUserDefinedTypesForDictionary) — CRUD опций в конструкторе пере-эмитит
    // пикер карточки; разовый getComponentOptions на эмит types оставлял stale.
    override fun flowAvailableComponentTypes(dictionaryId: Long): Flow<AvailableComponents> =
        lexemeApi.flowUserDefinedTypesForDictionary(dictionaryId).map { snapshot ->
            AvailableComponents(
                types = snapshot.types.map { it.toDomain() },
                optionsByType = snapshot.optionsByType
                    .mapKeys { me.apomazkin.lexeme.ComponentTypeId(it.key) }
                    .mapValues { (_, options) -> options.map { it.toDomain() } },
            )
        }

    override suspend fun getCaptionSuggestions(componentTypeId: ComponentTypeId): List<String> =
        lexemeApi.getCaptionSuggestions(componentTypeId.id)

    // ===== IS493 Э5: группы слова — тонкая делегация, маппинг Api→domain
    // (GroupApiEntity в screen-модуль не протекает, D7.3/ревью Arch-2) =====

    override fun wordGroups(wordId: Long): kotlinx.coroutines.flow.Flow<List<me.apomazkin.group.GroupNode>> =
        groupApi.wordGroups(wordId).map { groups ->
            groups.map {
                me.apomazkin.group.GroupNode(
                    id = it.id,
                    parentGroupId = it.parentGroupId,
                    name = it.name,
                )
            }
        }

    override fun dictGroups(dictionaryId: Long): kotlinx.coroutines.flow.Flow<List<me.apomazkin.group.GroupNode>> =
        groupApi.groupTree(dictionaryId).map { groups ->
            groups.map {
                me.apomazkin.group.GroupNode(
                    id = it.id,
                    parentGroupId = it.parentGroupId,
                    name = it.name,
                )
            }
        }

    override suspend fun addWordToGroup(
        wordId: Long,
        groupId: Long,
    ): me.apomazkin.group.AddMembershipOutcome =
        groupApi.addWordToGroup(wordId = wordId, groupId = groupId)

    override suspend fun removeWordFromGroup(
        wordId: Long,
        groupId: Long,
    ): me.apomazkin.group.RemoveMembershipOutcome =
        groupApi.removeWordFromGroup(wordId = wordId, groupId = groupId)

    private fun TemplateValues.trimmed(): TemplateValues = when (this) {
        is TextValues -> TextValues(Primitive.Text(value.value.trim()))
        // IS491: text trim; caption trim, пробельная → null (UC26).
        is me.apomazkin.lexeme.CaptionedTextValues -> me.apomazkin.lexeme.CaptionedTextValues(
            text = Primitive.Text(text.value.trim()),
            caption = caption?.value?.trim()?.takeIf { it.isNotEmpty() }?.let { Primitive.Text(it) },
        )
        else -> this
    }
}

fun TermApiEntity.toDomainEntity(): Term = Term(
    wordId = WordId(word.id),
    word = Word(word.value),
    dictionaryId = word.dictionaryId,
    addedDate = word.addDate,
    changedDate = word.changeDate,
    removedDate = word.removeDate,
    // newest-first для UI «новая лексема сверху».
    lexemeList = lexemes.sortedByDescending { it.addDate }.map { it.toDomain() },
)
