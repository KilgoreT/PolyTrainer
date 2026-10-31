package me.apomazkin.polytrainer.di.module.candidates

import kotlinx.coroutines.flow.first
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.ComponentOptionApiEntity
import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.lexeme.BatchComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.DictionaryForm
import me.apomazkin.lexeme.ExistingLemma
import me.apomazkin.lexeme.FormComponent
import me.apomazkin.lexeme.FormGroup
import me.apomazkin.lexeme.FormOption
import me.apomazkin.lexeme.SavedLexeme
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.lexemeTextKey
import me.apomazkin.lexeme.wordTextKey
import me.apomazkin.polytrainer.di.module.scancandidates.ComponentBranch
import me.apomazkin.polytrainer.di.module.scancandidates.ComponentTreeLoader
import me.apomazkin.polytrainer.di.module.scancandidates.flattened
import javax.inject.Inject

/** Контракт `getTermsByIds`: не больше 50 id за вызов (bind-лимит SQLite). */
private const val TERMS_CHUNK = 50

/** Есть ли ключ сервера в сборке — кнопка «Добавить лексемы-кандидаты» в карточке (WCL-08). */
data class ServerAccess(
    val hasKey: Boolean,
)

/**
 * Форма словаря для лексем-кандидатов — общая для экрана снимка (AWP: леммы
 * словаря с текстами лемм ответа) и диалога карточки слова (WCL-06: лексемы
 * одного слова): включённые живые компоненты всех зависимостей в порядке дерева,
 * опции каждого «Выбора», группы, леммы с текстами ядра, ключами текстов ядер,
 * датой добавления и сохранёнными лексемами (AWP-18, AWP-46). Компоненты и
 * их порядок даёт [componentTree] — тот же обход, что строит дерево запроса
 * (LFL-78).
 */
class CandidatesFormLoader @Inject constructor(
    private val groupApi: CoreDbApi.GroupApi,
    private val termApi: CoreDbApi.TermApi,
    private val wordsBatchApi: CoreDbApi.WordsBatchApi,
    private val componentTree: ComponentTreeLoader,
) {
    /** Экран снимка (AWP-07, AWP-18): леммы словаря с теми же текстами, что у лемм ответа. */
    suspend fun formOf(
        dictionary: DictionaryApiEntity,
        lemmaTexts: List<String>,
    ): DictionaryForm {
        val components = componentTree.componentsOf(dictionary.id).flattened()
        val shape = FormShape(components)
        val existing = existingLemmasOf(dictionary.id, lemmaTexts, shape)
        return baseForm(dictionary, components, existing)
    }

    /**
     * Леммы словаря с текстами [lemmaTexts] вне формы — для юзкейса по лемме
     * ответа, которой не было в списках (AWP-43.7); с группами, как у формы
     * снимка.
     */
    suspend fun lemmasOf(
        dictionary: DictionaryApiEntity,
        lemmaTexts: List<String>,
    ): List<ExistingLemma> {
        val components = componentTree.componentsOf(dictionary.id).flattened()
        return existingLemmasOf(dictionary.id, lemmaTexts, FormShape(components))
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
    ): DictionaryForm {
        val components = componentTree.componentsOf(dictionary.id).flattened()
        val shape = FormShape(components)
        val own = listOfNotNull(
            termApi.getTermById(wordId)?.toExistingLemma(shape, groupNames = emptyList()),
        )
        val lemmaWords = if (lemmaTexts.isEmpty()) {
            emptyList()
        } else {
            existingLemmasOf(dictionary.id, lemmaTexts, shape, withGroups = false)
                .filterNot { it.wordId == wordId }
        }
        return baseForm(dictionary, components, own + lemmaWords)
    }

    private suspend fun baseForm(
        dictionary: DictionaryApiEntity,
        components: List<ComponentBranch>,
        existing: List<ExistingLemma>,
    ): DictionaryForm {
        val groups = groupApi.groupTree(dictionary.id).first()
        return DictionaryForm(
            dictionaryId = dictionary.id,
            translationLanguage = dictionary.translationLanguage,
            components = components.map { it.type.toFormComponent() },
            options = components
                .filter { it.type.template == ComponentTemplate.CHOICE }
                .associate { branch ->
                    branch.type.id to branch.options.map { it.option.toFormOption() }
                },
            groups = groups.map { FormGroup(id = it.id, name = it.name) },
            existingLemmas = existing,
        )
    }

    private fun ComponentTypeApiEntity.toFormComponent(): FormComponent = FormComponent(
        id = id,
        systemKey = systemKey,
        name = name,
        template = template,
        isCore = core,
        isMultiple = isMultiple,
        position = position,
        dependsOn = dependsOn,
    )

    private fun ComponentOptionApiEntity.toFormOption(): FormOption = FormOption(
        id = id,
        key = systemKey,
        label = label,
        position = position,
    )

    // Леммы словаря с тем же текстом (без регистра, краёв и формы Unicode) —
    // лёгким запросом текстов, затем контент пачками по 50; порядок — самая
    // новая первой (id DESC, порядок списка «Слова»).
    private suspend fun existingLemmasOf(
        dictionaryId: Long,
        lemmaTexts: List<String>,
        shape: FormShape,
        withGroups: Boolean = true,
    ): List<ExistingLemma> {
        val keys = lemmaTexts.mapTo(HashSet()) { wordTextKey(it) }
        val ids = wordsBatchApi
            .getWordTexts(dictionaryId)
            .filter { wordTextKey(it.value) in keys }
            .map { it.id }
        val terms = ids
            .chunked(TERMS_CHUNK)
            .flatMap { termApi.getTermsByIds(it) }
            .sortedByDescending { it.word.id }
        return terms.map { term ->
            val groupNames = if (withGroups) {
                groupApi.wordGroups(term.word.id).first().map { it.name }
            } else {
                emptyList()
            }
            term.toExistingLemma(shape, groupNames)
        }
    }

    private fun TermApiEntity.toExistingLemma(
        shape: FormShape,
        groupNames: List<String>,
    ): ExistingLemma = ExistingLemma(
        wordId = word.id,
        text = word.value,
        lexemeCount = lexemes.size,
        coreValues = coreValues(shape.firstCoreId),
        lexemeTextKeys = coreTextKeys(shape.coreIds),
        groupNames = groupNames,
        addedAt = word.addDate.time,
        lexemes = savedLexemes(shape.componentIds),
    )

    /**
     * Лексемы слова с живыми значениями компонентов формы, по id типов, для
     * карточки слова «в словаре» (AWP-46); значения выключенных компонентов не
     * идут.
     */
    private fun TermApiEntity.savedLexemes(componentIds: Set<Long>): List<SavedLexeme> =
        lexemes.map { lexeme ->
            SavedLexeme(
                id = lexeme.id,
                components = lexeme.components
                    .filter { it.removedAt == null && it.type.id in componentIds }
                    .map { BatchComponent(typeId = it.type.id, data = it.data) },
            )
        }

    /**
     * Очертания формы: [firstCoreId] — первое включённое ядро шаблона «текст»
     * (встроенные раньше своих, затем position, затем id; AWP-06, AWP-18.7),
     * [coreIds] — все такие ядра, [componentIds] — все компоненты формы.
     */
    private class FormShape(components: List<ComponentBranch>) {
        private val cores = components
            .map { it.type }
            .filter { it.template == ComponentTemplate.TEXT && it.core }
            .sortedWith(compareBy({ it.systemKey == null }, { it.position }, { it.id }))
        val firstCoreId: Long? = cores.firstOrNull()?.id
        val coreIds: Set<Long> = cores.mapTo(HashSet()) { it.id }
        val componentIds: Set<Long> = components.mapTo(HashSet()) { it.type.id }
    }

    private fun TermApiEntity.coreValues(coreTypeId: Long?): List<String> =
        lexemes.mapNotNull { lexeme ->
            lexeme.components
                .firstOrNull { it.type.id == coreTypeId && it.removedAt == null }
                ?.data
                ?.let { it as? TextValues }
                ?.value
                ?.value
        }

    /**
     * Ключи значений включённых ядер шаблона «текст» [coreIds] всех лексем слова:
     * пометка «есть» — только по ним. Параметры вроде «Толкования» и значения
     * выключенных ядер не сравниваются (Д42, AWP-07, AWP-07.11).
     */
    private fun TermApiEntity.coreTextKeys(coreIds: Set<Long>): Set<String> = lexemes
        .flatMap { lexeme -> lexeme.components }
        .filter { it.removedAt == null && it.type.id in coreIds }
        .mapNotNull { (it.data as? TextValues)?.value?.value }
        .mapTo(HashSet()) { lexemeTextKey(it) }
}
