package me.apomazkin.wordcard.mate

import androidx.compose.runtime.Stable
import me.apomazkin.mate.Effect
import me.apomazkin.lexeme.Lexeme
import java.util.Date

const val NOT_IN_DB = -1L

@Stable
data class WordCardState(
    val topBarState: TopBarState = TopBarState(),
    val isLoading: Boolean = true,
    val isPendingDbOp: Boolean = false,
    val wordState: WordState = WordState.NotLoaded,
    val lexemeList: List<LexemeState> = listOf(),
    val lexemeIdPendingDelete: Long? = null,
) {
    val isLoaded: Boolean
        get() = when (wordState) {
            is WordState.Loaded -> true
            WordState.NotLoaded -> false
        }

    val isCreatingLexeme: Boolean
        get() = lexemeList.any { it.id == NOT_IN_DB }

    val canAddLexeme: Boolean
        get() = !isPendingDbOp && !isCreatingLexeme
}

@Stable
data class TopBarState(
    val isMenuOpen: Boolean = false
)

@Stable
sealed interface WordState {
    data object NotLoaded : WordState

    data class Loaded(
        val id: Long,
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
    val translation: TextValueState? = null,
    val definition: TextValueState? = null,
) {
    val canAddTranslation: Boolean get() = translation == null
    val canAddDefinition: Boolean get() = definition == null
}

@Stable
data class TextValueState(
    val isEdit: Boolean = false,
    val origin: String = "",
    val edited: String = "",
)

/**
 * ###### TOP BAR STATE ######
 */
fun WordCardState.showMenu(): WordCardState =
    this.copy(
        topBarState = this.topBarState.copy(
            isMenuOpen = true
        )
    )

fun WordCardState.hideMenu(): WordCardState =
    this.copy(
        topBarState = this.topBarState.copy(
            isMenuOpen = false
        )
    )

/**
 * ###### WORD STATE ######
 */
fun WordCardState.enableWordEdit(): WordCardState {
    val loaded = wordState as? WordState.Loaded ?: return this
    return this.copy(
        wordState = loaded.copy(
            isEditMode = true,
            edited = loaded.value
        )
    )
}

fun WordCardState.disableWordEdit(): WordCardState {
    val loaded = wordState as? WordState.Loaded ?: return this
    return this.copy(
        wordState = loaded.copy(
            isEditMode = false,
            edited = ""
        )
    )
}

fun WordCardState.updateWordEdited(edited: String): WordCardState {
    val loaded = wordState as? WordState.Loaded ?: return this
    return this.copy(
        wordState = loaded.copy(
            edited = edited
        )
    )
}

fun WordCardState.showWordWarningDialog(): WordCardState {
    val loaded = wordState as? WordState.Loaded ?: return this
    return this.copy(
        wordState = loaded.copy(
            showWarningDialog = true
        )
    )
}

fun WordCardState.hideWordWarningDialog(): WordCardState {
    val loaded = wordState as? WordState.Loaded ?: return this
    return this.copy(
        wordState = loaded.copy(
            showWarningDialog = false
        )
    )
}

/**
 * ###### CLOSE ALL EDIT MODES ######
 *
 * Закрывает word edit + все chip-edits.
 */
fun WordCardState.closeAllEditModes(): WordCardState {
    val newWordState: WordState = when (val w = wordState) {
        is WordState.Loaded -> w.copy(isEditMode = false, edited = "")
        WordState.NotLoaded -> w
    }
    val newList = lexemeList.map { l ->
        l.copy(
            translation = l.translation?.copy(isEdit = false, edited = ""),
            definition = l.definition?.copy(isEdit = false, edited = ""),
        )
    }
    return copy(wordState = newWordState, lexemeList = newList)
}

/**
 * ###### COMMIT AND CLOSE ALL EDIT MODES ######
 *
 * Для каждого активного edit с грязным `edited` (!= origin && не пустой) эмитит
 * `Update*`-эффект и локально комитит; пустые/неизменённые — просто закрываются.
 */
fun WordCardState.commitAndCloseAllEdits(): Pair<WordCardState, Set<Effect>> {
    val loaded = wordState as? WordState.Loaded
        ?: return closeAllEditModes() to emptySet()

    val effects = mutableSetOf<Effect>()

    // Word commit / close.
    val wordEdited = loaded.edited
    val wordOrigin = loaded.value
    val newWordState: WordState =
        if (loaded.isEditMode && wordEdited.isNotEmpty() && wordEdited != wordOrigin) {
            effects += DatasourceEffect.UpdateWord(
                wordId = loaded.id,
                value = wordEdited,
            )
            loaded.copy(
                isEditMode = false,
                edited = "",
                value = wordEdited,
            )
        } else {
            loaded.copy(isEditMode = false, edited = "")
        }

    // Lexeme list — translation / definition commit per item.
    val newList = lexemeList.map { l ->
        val newTranslation = l.translation?.let { t ->
            if (t.isEdit && t.edited.isNotEmpty() && t.edited != t.origin) {
                val effectLexemeId: Long? = if (l.id == NOT_IN_DB) null else l.id
                effects += DatasourceEffect.UpdateLexemeTranslation(
                    wordId = loaded.id,
                    lexemeId = effectLexemeId,
                    translation = t.edited,
                )
                t.copy(origin = t.edited, isEdit = false, edited = "")
            } else {
                t.copy(isEdit = false, edited = "")
            }
        }
        val newDefinition = l.definition?.let { d ->
            if (d.isEdit && d.edited.isNotEmpty() && d.edited != d.origin) {
                val effectLexemeId: Long? = if (l.id == NOT_IN_DB) null else l.id
                effects += DatasourceEffect.UpdateLexemeDefinition(
                    wordId = loaded.id,
                    lexemeId = effectLexemeId,
                    definition = d.edited,
                )
                d.copy(origin = d.edited, isEdit = false, edited = "")
            } else {
                d.copy(isEdit = false, edited = "")
            }
        }
        l.copy(translation = newTranslation, definition = newDefinition)
    }

    val pending = effects.isNotEmpty()
    val newState = copy(
        wordState = newWordState,
        lexemeList = newList,
        isPendingDbOp = if (pending) true else isPendingDbOp,
    )
    return newState to effects
}

/**
 * ###### LEXEME LIST ######
 */
fun WordCardState.updateLexeme(
    lexemeId: Long,
    update: (LexemeState) -> LexemeState
): WordCardState =
    this.copy(
        lexemeList = this.lexemeList.map { lexeme ->
            if (lexeme.id == lexemeId) update(lexeme) else lexeme
        }
    )

fun WordCardState.removeLexeme(lexemeId: Long): WordCardState =
    this.copy(
        lexemeList = this.lexemeList.filter { it.id != lexemeId }
    )

/**
 * ###### ENTITY MAPPING ######
 */
fun Lexeme.toLexemeState(): LexemeState = LexemeState(
    id = this.lexemeId.id,
    translation = this.translation?.let { translation ->
        TextValueState(
            origin = translation.value,
            isEdit = false,
            edited = "",
        )
    },
    definition = this.definition?.let { definition ->
        TextValueState(
            origin = definition.value,
            isEdit = false,
            edited = "",
        )
    },
)

/**
 * ###### SPECIALIZED LEXEME EXTENSIONS ######
 */

// Translation management
fun WordCardState.createLexemeTranslation(lexemeId: Long): WordCardState =
    this.updateLexeme(lexemeId) {
        it.copy(
            translation = TextValueState(
                origin = "",
                isEdit = true,
                edited = "",
            )
        )
    }

fun WordCardState.updateLexemeTranslationText(lexemeId: Long, text: String): WordCardState =
    this.updateLexeme(lexemeId) {
        it.copy(translation = it.translation?.copy(edited = text))
    }

fun WordCardState.enableLexemeTranslationEdit(lexemeId: Long): WordCardState =
    this.updateLexeme(lexemeId) {
        it.copy(
            translation = it.translation?.copy(
                isEdit = true,
                edited = it.translation.origin,
            )
        )
    }

// Definition management
fun WordCardState.createLexemeDefinition(lexemeId: Long): WordCardState =
    this.updateLexeme(lexemeId) {
        it.copy(
            definition = TextValueState(
                origin = "",
                isEdit = true,
                edited = "",
            )
        )
    }

fun WordCardState.updateLexemeDefinitionText(lexemeId: Long, text: String): WordCardState =
    this.updateLexeme(lexemeId) {
        it.copy(definition = it.definition?.copy(edited = text))
    }

fun WordCardState.enableLexemeDefinitionEdit(lexemeId: Long): WordCardState =
    this.updateLexeme(lexemeId) {
        it.copy(
            definition = it.definition?.copy(
                isEdit = true,
                edited = it.definition.origin,
            )
        )
    }

