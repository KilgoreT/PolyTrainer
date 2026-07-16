package me.apomazkin.wordcard.mate

import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValue
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.LexemeId
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TextValues
import me.apomazkin.wordcard.entity.Term
import me.apomazkin.wordcard.entity.Word
import me.apomazkin.wordcard.entity.WordId
import java.util.Date

/** Произвольный @StringRes Int для error-снеков в тестах (значение неважно). */
internal const val R_STRING_X = 123

/**
 * Заглушка нового generic [me.apomazkin.wordcard.deps.WordCardUseCase] — все методы кидают.
 * Для тестов, где нужен только один метод (через делегирование `by`).
 */
internal object NotImplementedUseCase : me.apomazkin.wordcard.deps.WordCardUseCase {
    override suspend fun getTermById(wordId: Long): Term? = TODO()
    override suspend fun deleteWord(wordId: Long): Int = TODO()
    override suspend fun updateWord(wordId: Long, value: String): Boolean = TODO()
    override suspend fun deleteLexeme(wordId: Long, lexemeId: Long): me.apomazkin.wordcard.deps.RemoveLexemeResult? = TODO()
    override suspend fun addLexemeWithComponent(wordId: Long, dictionaryId: Long, ref: ComponentTypeRef, data: me.apomazkin.lexeme.TemplateValues): Lexeme? = TODO()
    override suspend fun addComponentValue(lexemeId: Long, componentTypeId: ComponentTypeId, data: me.apomazkin.lexeme.TemplateValues): me.apomazkin.wordcard.deps.AddComponentValueResult? = TODO()
    override suspend fun updateComponentValue(componentValueId: ComponentValueId, lexemeId: Long, data: me.apomazkin.lexeme.TemplateValues): Lexeme? = TODO()
    override suspend fun deleteComponentValue(componentValueId: ComponentValueId, lexemeId: Long): me.apomazkin.wordcard.deps.RemoveComponentResult? = TODO()
    override suspend fun restoreLexemeWithComponents(wordId: Long, dictionaryId: Long, snapshot: Lexeme): Lexeme? = TODO()
    override fun flowAvailableComponentTypes(dictionaryId: Long): kotlinx.coroutines.flow.Flow<List<ComponentType>> = TODO()
}

/**
 * Общие фикстуры для reducer-тестов IS481 (07 §1).
 *
 * TDD: ссылается на БУДУЩИЙ API (новый shape `WordCardState`/`LexemeState`/
 * `ComponentValueState`, `ComponentValueKey`) — до реализации продакшна НЕ компилируется.
 */

internal val TR = ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION)

internal fun loaded(
    wordId: Long = 7L,
    dictionaryId: Long = 3L,
    lexemes: List<LexemeState> = emptyList(),
    isPendingDbOp: Boolean = false,
    isExiting: Boolean = false,
    availableTypes: List<ComponentType> = emptyList(),
    nextPristineKey: Long = 1L,
): WordCardState = WordCardState(
    isLoading = false,
    isPendingDbOp = isPendingDbOp,
    isExiting = isExiting,
    wordState = WordState.Loaded(
        id = wordId,
        dictionaryId = dictionaryId,
        added = Date(0L),
        value = "w",
    ),
    lexemeList = lexemes,
    availableComponentTypes = availableTypes,
    nextPristineKey = nextPristineKey,
)

internal fun savedCv(
    id: Long,
    typeId: Long = 50L,
    ref: ComponentTypeRef = TR,
    origin: String = "",
    isMultiple: Boolean = false,
    isEdit: Boolean = false,
    isCommitting: Boolean = false,
    edited: String = "",
): ComponentValueState = ComponentValueState(
    key = ComponentValueKey.Saved(ComponentValueId(id)),
    componentTypeId = ComponentTypeId(typeId),
    componentTypeRef = ref,
    isMultiple = isMultiple,
    isEdit = isEdit,
    isCommitting = isCommitting,
    origin = origin,
    edited = edited,
)

internal fun pristineCv(
    key: Long,
    typeId: Long = 50L,
    ref: ComponentTypeRef = TR,
    isMultiple: Boolean = false,
    isCommitting: Boolean = false,
    edited: String = "",
): ComponentValueState = ComponentValueState(
    key = ComponentValueKey.Pristine(key),
    componentTypeId = ComponentTypeId(typeId),
    componentTypeRef = ref,
    isMultiple = isMultiple,
    isEdit = true,
    isCommitting = isCommitting,
    origin = "",
    edited = edited,
)

internal fun lexeme(id: Long, components: List<ComponentValueState>): LexemeState =
    LexemeState(id = id, components = components)

/** Доменный `ComponentType` для availableTypes / payload (07 §9.2). */
internal fun ctype(
    id: Long = 50L,
    ref: ComponentTypeRef = TR,
    isMultiple: Boolean = false,
    template: ComponentTemplate = ComponentTemplate.TEXT,
): ComponentType {
    val systemKey = (ref as? ComponentTypeRef.BuiltIn)?.key
    val name = (ref as? ComponentTypeRef.UserDefined)?.name
    return ComponentType(
        id = ComponentTypeId(id),
        systemKey = systemKey,
        dictionaryId = if (systemKey == null) 3L else null,
        name = name,
        template = template,
        position = 0,
        isMultiple = isMultiple,
        createdAt = Date(0L),
        updatedAt = Date(0L),
        removedAt = null,
    )
}

/** Доменный `ComponentValue` (payload `RefreshLexemeComponents` — re-read из БД). */
internal fun domainCv(
    id: Long,
    lexemeId: Long,
    text: String,
    typeId: Long = 50L,
    ref: ComponentTypeRef = TR,
    isMultiple: Boolean = false,
): ComponentValue = ComponentValue(
    id = ComponentValueId(id),
    lexemeId = LexemeId(lexemeId),
    type = ctype(typeId, ref, isMultiple),
    data = TextValues(Primitive.Text(text)),
)

/** Доменная `Lexeme` для payload `LexemeDraftPromoted.newLexeme` / `WordLoaded`. */
internal fun domainLexeme(id: Long, comps: List<ComponentValue>): Lexeme = Lexeme(
    lexemeId = LexemeId(id),
    components = comps,
    addDate = Date(0L),
    changeDate = null,
)

/** Минимальный `Term` для `RefreshWord`/`WordLoaded`. */
internal fun stubTerm(
    wordId: Long = 7L,
    dictionaryId: Long = 3L,
    value: String = "w",
    lexemes: List<Lexeme> = emptyList(),
    dictionaryFlagRes: Int? = null,
): Term = Term(
    wordId = WordId(wordId),
    word = Word(value),
    dictionaryId = dictionaryId,
    dictionaryFlagRes = dictionaryFlagRes,
    addedDate = Date(0L),
    changedDate = null,
    removedDate = null,
    lexemeList = lexemes,
)
