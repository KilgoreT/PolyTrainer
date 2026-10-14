package me.apomazkin.polytrainer.di.module.candidates

import kotlinx.coroutines.flow.first
import me.apomazkin.candidates.PhotoComponent
import me.apomazkin.candidates.PhotoDictionaryForm
import me.apomazkin.candidates.PhotoExistingLemma
import me.apomazkin.candidates.PhotoGroup
import me.apomazkin.candidates.PhotoPartOfSpeechOption
import me.apomazkin.candidates.PhotoTemplate
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.lexemeTextKey
import me.apomazkin.lexeme.wordTextKey

/** Контракт `getTermsByIds`: не больше 50 id за вызов (bind-лимит SQLite). */
private const val TERMS_CHUNK = 50

/** Есть ли ключ сервера в сборке — кнопка «Добавить значения из словаря» в карточке (WCL-08). */
data class ServerAccess(
    val hasKey: Boolean,
)

/**
 * Форма словаря для лексем-кандидатов — общая для окна снимка (AWP: леммы
 * словаря с текстами лемм ответа) и диалога карточки слова (WCL-06: лексемы
 * одного слова): включённые живые компоненты лексемы, опции «Части речи»,
 * группы, леммы с текстами ядра и ключами текстов лексем.
 */
class CandidatesFormLoader(
    private val lexemeApi: CoreDbApi.LexemeApi,
    private val groupApi: CoreDbApi.GroupApi,
    private val termApi: CoreDbApi.TermApi,
    private val wordsBatchApi: CoreDbApi.WordsBatchApi,
) {
    /** Окно снимка (AWP-07, AWP-18): леммы словаря с теми же текстами, что у лемм ответа. */
    suspend fun formOf(dictionary: DictionaryApiEntity, lemmaTexts: List<String>): PhotoDictionaryForm {
        val types = lexemeTypes(dictionary.id)
        return baseForm(dictionary, types, existingLemmasOf(dictionary.id, lemmaTexts, types))
    }

    /**
     * Диалог карточки (WCL-06, WCL-15): леммы формы — слово диалога (его уже
     * нет — без него) и слова словаря с текстами лемм-форм ответа
     * [lemmaTexts] — получатели переноса, самая новая первой; без групп.
     */
    suspend fun formForWord(
        dictionary: DictionaryApiEntity,
        wordId: Long,
        lemmaTexts: List<String> = emptyList(),
    ): PhotoDictionaryForm {
        val types = lexemeTypes(dictionary.id)
        val own = listOfNotNull(termApi.getTermById(wordId)?.toExistingLemma(firstTextCoreId(types), groupNames = emptyList()))
        val lemmaWords = if (lemmaTexts.isEmpty()) {
            emptyList()
        } else {
            existingLemmasOf(dictionary.id, lemmaTexts, types, withGroups = false).filterNot { it.wordId == wordId }
        }
        return baseForm(dictionary, types, own + lemmaWords)
    }

    private suspend fun lexemeTypes(dictionaryId: Long): List<ComponentTypeApiEntity> =
        lexemeApi.getComponentTypes(dictionaryId).filter { it.enabled && it.dependsOn == DependencyTarget.Lexeme }

    private suspend fun baseForm(
        dictionary: DictionaryApiEntity,
        types: List<ComponentTypeApiEntity>,
        existing: List<PhotoExistingLemma>,
    ): PhotoDictionaryForm {
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
            existingLemmas = existing,
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

    // Леммы словаря с тем же текстом (без регистра, краёв и формы Unicode) —
    // лёгким запросом текстов, затем контент пачками по 50; порядок — самая
    // новая первой (id DESC, порядок списка «Слова»).
    private suspend fun existingLemmasOf(
        dictionaryId: Long,
        lemmaTexts: List<String>,
        types: List<ComponentTypeApiEntity>,
        withGroups: Boolean = true,
    ): List<PhotoExistingLemma> {
        val keys = lemmaTexts.mapTo(HashSet()) { wordTextKey(it) }
        val ids = wordsBatchApi.getWordTexts(dictionaryId).filter { wordTextKey(it.value) in keys }.map { it.id }
        val terms = ids.chunked(TERMS_CHUNK).flatMap { termApi.getTermsByIds(it) }.sortedByDescending { it.word.id }
        val coreTypeId = firstTextCoreId(types)
        return terms.map { term ->
            val groupNames = if (withGroups) groupApi.wordGroups(term.word.id).first().map { it.name } else emptyList()
            term.toExistingLemma(coreTypeId, groupNames)
        }
    }

    private fun TermApiEntity.toExistingLemma(
        coreTypeId: Long?,
        groupNames: List<String>,
    ): PhotoExistingLemma = PhotoExistingLemma(
        wordId = word.id,
        text = word.value,
        lexemeCount = lexemes.size,
        coreValues = coreValues(coreTypeId),
        lexemeTextKeys = textKeys(),
        groupNames = groupNames,
    )

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
}
