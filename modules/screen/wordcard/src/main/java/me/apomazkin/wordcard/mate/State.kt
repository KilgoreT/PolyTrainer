package me.apomazkin.wordcard.mate

import androidx.compose.runtime.Stable
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.ComponentOption
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentValue
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.toRef
import me.apomazkin.mate.Effect
import java.util.Date

const val NOT_IN_DB = -1L

@Stable
data class WordCardState(
    val topBarState: TopBarState = TopBarState(),
    val isLoading: Boolean = true,
    val isPendingDbOp: Boolean = false,
    /** Flush-on-back (маркеры, §6.2.3): «назад» при незавершённой записи — лоадер + отложенная навигация. */
    val isExiting: Boolean = false,
    val wordState: WordState = WordState.NotLoaded,
    val lexemeList: List<LexemeState> = listOf(),
    val lexemeIdPendingDelete: Long? = null,
    /** Available component types словаря (driver для ChipsRow). */
    val availableComponentTypes: List<ComponentType> = emptyList(),
    /** IS486: живые опции CHOICE-типов словаря (display: label ?: ресурс по systemKey). */
    val optionsByType: Map<ComponentTypeId, List<ComponentOption>> = emptyMap(),
    /** Reducer-counter для уникальных pristine identity. */
    val nextPristineKey: Long = 1L,
) {
    val isLoaded: Boolean
        get() = wordState is WordState.Loaded

    val isCreatingLexeme: Boolean
        get() = lexemeList.any { it.id == NOT_IN_DB }

    val canAddLexeme: Boolean
        get() = !isPendingDbOp && !isCreatingLexeme

    /** computed: есть хоть одна незавершённая (in-flight) запись компонента — для flush-on-back. */
    val hasInFlightCommits: Boolean
        get() = lexemeList.any { l -> l.components.any { it.isCommitting } }
}

@Stable
data class TopBarState(
    val isMenuOpen: Boolean = false,
)

@Stable
sealed interface WordState {
    data object NotLoaded : WordState

    data class Loaded(
        val id: Long,
        val dictionaryId: Long,
        /** IS485 — drawable флага словаря для шапки; null → плейсхолдер. */
        val dictionaryFlagRes: Int? = null,
        val added: Date,
        val value: String,
        val isEditMode: Boolean = false,
        val edited: String = "",
        val showWarningDialog: Boolean = false,
    ) : WordState
}

@Stable
data class LexemeState(
    val id: Long = NOT_IN_DB,
    val components: List<ComponentValueState> = emptyList(),
) {
    /** typeId всех НЕ-multi компонентов — для скрытия их chip'ов в ChipsRow. */
    val addedNonMultipleTypeIds: Set<ComponentTypeId>
        get() = components.filterNot { it.isMultiple }.map { it.componentTypeId }.toSet()
}

@Stable
data class ComponentValueState(
    val key: ComponentValueKey,
    val componentTypeId: ComponentTypeId,
    val componentTypeRef: ComponentTypeRef,
    val isMultiple: Boolean,
    /** IS486: шаблон компонента — ветвление рендера/коммита (TEXT-поле vs CHOICE-пикер). */
    val template: ComponentTemplate = ComponentTemplate.TEXT,
    val isEdit: Boolean = false,
    /** A10-redesign: DB-операция в полёте + flush-on-back driver (hasInFlightCommits). */
    val isCommitting: Boolean = false,
    val origin: String = "",
    val edited: String = "",
    /** IS486 CHOICE: сохранённый выбор (аналог origin) — id опции. */
    val selectedOptionId: Long? = null,
) {
    val isPristine: Boolean get() = key is ComponentValueKey.Pristine
    val componentValueId: ComponentValueId? get() = (key as? ComponentValueKey.Saved)?.componentValueId
    val pristineKey: Long? get() = (key as? ComponentValueKey.Pristine)?.pristineKey
}

/**
 * ###### TOP BAR ######
 */
fun WordCardState.showMenu(): WordCardState =
    copy(topBarState = topBarState.copy(isMenuOpen = true))

fun WordCardState.hideMenu(): WordCardState =
    copy(topBarState = topBarState.copy(isMenuOpen = false))

/**
 * ###### WORD STATE ######
 */
fun WordCardState.enableWordEdit(): WordCardState {
    val loaded = wordState as? WordState.Loaded ?: return this
    return copy(wordState = loaded.copy(isEditMode = true, edited = loaded.value))
}

fun WordCardState.disableWordEdit(): WordCardState {
    val loaded = wordState as? WordState.Loaded ?: return this
    return copy(wordState = loaded.copy(isEditMode = false, edited = ""))
}

fun WordCardState.updateWordEdited(edited: String): WordCardState {
    val loaded = wordState as? WordState.Loaded ?: return this
    return copy(wordState = loaded.copy(edited = edited))
}

fun WordCardState.showWordWarningDialog(): WordCardState {
    val loaded = wordState as? WordState.Loaded ?: return this
    return copy(wordState = loaded.copy(showWarningDialog = true))
}

fun WordCardState.hideWordWarningDialog(): WordCardState {
    val loaded = wordState as? WordState.Loaded ?: return this
    return copy(wordState = loaded.copy(showWarningDialog = false))
}

/**
 * ###### LEXEME LIST ######
 */
fun WordCardState.updateLexeme(lexemeId: Long, update: (LexemeState) -> LexemeState): WordCardState =
    copy(lexemeList = lexemeList.map { if (it.id == lexemeId) update(it) else it })

fun WordCardState.removeLexeme(lexemeId: Long): WordCardState =
    copy(lexemeList = lexemeList.filter { it.id != lexemeId })

/**
 * ###### LEXEME COMPONENT EXTENSIONS ######
 */
fun LexemeState.findByKey(key: ComponentValueKey): ComponentValueState? =
    components.firstOrNull { it.key == key }

fun LexemeState.updateComponent(key: ComponentValueKey, transform: (ComponentValueState) -> ComponentValueState): LexemeState =
    copy(components = components.map { if (it.key == key) transform(it) else it })

fun LexemeState.removeComponent(key: ComponentValueKey): LexemeState =
    copy(components = components.filterNot { it.key == key })

fun LexemeState.appendPristine(component: ComponentValueState): LexemeState =
    copy(components = components + component)

/**
 * ###### ENTITY MAPPING ######
 */
fun ComponentValue.toComponentValueState(): ComponentValueState = ComponentValueState(
    key = ComponentValueKey.Saved(id),
    componentTypeId = type.id,
    componentTypeRef = type.toRef(),
    isMultiple = type.isMultiple,
    template = type.template,
    origin = data.asText().orEmpty(),
    selectedOptionId = (data as? ChoiceValues)?.optionId,
)

fun Lexeme.toLexemeState(): LexemeState = LexemeState(
    id = lexemeId.id,
    components = components.map { it.toComponentValueState() },
)

/**
 * ###### IS486: ПРАВИЛО УЧАСТИЯ (spec §6) ######
 * Модельная функция (дом-прецедент — computed addedNonMultipleTypeIds):
 * typeId компонентов, доступных лексеме для добавления.
 * Черновик (NOT_IN_DB): только ядра. Реальная лексема: цель типа активна:
 * - Lexeme-цель: ядро — всегда; не-ядро — лексема оформлена (реальная);
 * - Component(t): есть сохранённое значение типа t;
 * - Option(o): есть сохранённый CHOICE-выбор опции o.
 * Минус уже добавленные не-multi (существующее правило чипов).
 * Минус disabled (spec §6: не предлагается для добавления новых значений;
 * существующие значения живут — их State не трогает).
 */
fun WordCardState.addableTypeIdsFor(lexeme: LexemeState): Set<ComponentTypeId> =
    availableComponentTypes.filter { type ->
        type.enabled && isTargetActiveFor(lexeme, type) && type.id !in lexeme.addedNonMultipleTypeIds
    }.map { it.id }.toSet()

private fun isTargetActiveFor(lexeme: LexemeState, type: ComponentType): Boolean =
    when (val target = type.dependsOn) {
        DependencyTarget.Lexeme ->
            if (type.core) true else lexeme.id != NOT_IN_DB

        is DependencyTarget.Component ->
            lexeme.id != NOT_IN_DB && lexeme.components.any {
                it.componentTypeId == target.typeId && !it.isPristine
            }

        is DependencyTarget.Option ->
            lexeme.id != NOT_IN_DB && lexeme.components.any {
                !it.isPristine && it.selectedOptionId == target.optionId
            }
    }

/**
 * ###### COMMIT ALL EDIT MODES ######
 */
/**
 * Commit всех правок (flush): для каждого компонента — [commitDecision], эмиссия эффектов.
 * - NoOp → закрыть edit; LocalRemove → drop локально (без эффекта);
 * - Update → A10 hold edit + isCommitting=true, эффект Upsert (UpdateValue/AddValue);
 * - PessimisticRemove → isCommitting=true, эффект RemoveComponentValue;
 * - NOT_IN_DB лексема → только anchor (первый по порядку Update) эмитит CreateLexeme,
 *   остальные ждут promotion; пустой draft удаляется.
 * - word edit (изменён) → UpdateWord + value продвигается.
 */
fun WordCardState.commitAndCloseAllEdits(): Pair<WordCardState, Set<Effect>> {
    val loaded = wordState as? WordState.Loaded ?: return this to emptySet()
    val effects = mutableSetOf<Effect>()

    val newLexemes = lexemeList.mapNotNull { lexeme ->
        if (lexeme.id == NOT_IN_DB) {
            commitDraftLexeme(lexeme, loaded.id, loaded.dictionaryId, effects)
        } else {
            commitRealLexeme(lexeme, loaded.id, loaded.dictionaryId, effects)
        }
    }

    val trimmedWord = loaded.edited.trim()
    val newWordState = if (loaded.isEditMode && trimmedWord.isNotEmpty() && trimmedWord != loaded.value) {
        effects += DatasourceEffect.UpdateWord(loaded.id, trimmedWord)
        loaded.copy(value = trimmedWord, isEditMode = false, edited = "")
    } else {
        loaded.copy(isEditMode = false, edited = "")
    }

    return copy(
        lexemeList = newLexemes,
        wordState = newWordState,
        isPendingDbOp = isPendingDbOp || effects.isNotEmpty(),
    ) to effects
}

private fun commitRealLexeme(
    lexeme: LexemeState,
    wordId: Long,
    dictionaryId: Long,
    effects: MutableSet<Effect>,
): LexemeState {
    val newComps = lexeme.components.mapNotNull { cv ->
        if (cv.isCommitting) return@mapNotNull cv // уже in-flight — не реэмитим
        when (val outcome = cv.commitDecision()) {
            CommitOutcome.NoOp -> cv.copy(isEdit = false, edited = "")
            CommitOutcome.LocalRemove -> null
            CommitOutcome.PessimisticRemove -> {
                val cvId = cv.componentValueId ?: return@mapNotNull null
                effects += DatasourceEffect.RemoveComponentValue(cvId, lexeme.id)
                cv.copy(isCommitting = true)
            }
            is CommitOutcome.Update -> {
                val cvId = cv.componentValueId
                if (cvId != null) {
                    effects += DatasourceEffect.UpsertComponentValue.UpdateValue(
                        wordId = wordId,
                        dictionaryId = dictionaryId,
                        lexemeId = lexeme.id,
                        componentValueId = cvId,
                        componentTypeId = cv.componentTypeId,
                        componentTypeRef = cv.componentTypeRef,
                        data = textValuesOf(outcome.text),
                    )
                } else {
                    effects += DatasourceEffect.UpsertComponentValue.AddValue(
                        wordId = wordId,
                        dictionaryId = dictionaryId,
                        lexemeId = lexeme.id,
                        pristineKey = cv.pristineKey!!,
                        componentTypeId = cv.componentTypeId,
                        componentTypeRef = cv.componentTypeRef,
                        data = textValuesOf(outcome.text),
                    )
                }
                cv.copy(isCommitting = true)
            }
        }
    }
    return lexeme.copy(components = newComps)
}

private fun commitDraftLexeme(
    lexeme: LexemeState,
    wordId: Long,
    dictionaryId: Long,
    effects: MutableSet<Effect>,
): LexemeState? {
    if (lexeme.components.any { it.isCommitting }) return lexeme // draft уже создаётся — не реэмитим
    val survived = lexeme.components.filterNot { it.commitDecision() == CommitOutcome.LocalRemove }
    if (survived.isEmpty()) return null
    val anchor = survived.firstOrNull { it.commitDecision() is CommitOutcome.Update }
        ?: return lexeme.copy(components = survived)
    val anchorText = (anchor.commitDecision() as CommitOutcome.Update).text
    effects += DatasourceEffect.UpsertComponentValue.CreateLexeme(
        wordId = wordId,
        dictionaryId = dictionaryId,
        pristineKey = anchor.pristineKey!!,
        componentTypeId = anchor.componentTypeId,
        componentTypeRef = anchor.componentTypeRef,
        data = textValuesOf(anchorText),
    )
    val newComps = survived.map { cv ->
        if (cv.key == anchor.key) cv.copy(isCommitting = true) else cv
    }
    return lexeme.copy(components = newComps)
}
