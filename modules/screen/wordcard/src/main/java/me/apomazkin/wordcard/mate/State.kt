package me.apomazkin.wordcard.mate

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.Term
import java.util.Date

const val NOT_IN_DB = -1L

@Stable
data class WordCardState(
    val topBarState: TopBarState = TopBarState(),
    val addLexemeBottomState: AddLexemeBottomState = AddLexemeBottomState(),
    val closeScreen: Boolean = false,
    val isLoading: Boolean = true,
    val wordState: WordState = WordState(),
    val lexemeList: List<LexemeState> = listOf(),
    val snackbarState: SnackbarState = SnackbarState()
)

@Stable
data class TopBarState(
    val isMenuOpen: Boolean = false
)

@Stable
data class AddLexemeBottomState(
    val show: Boolean = false,
    val isTranslationCheck: Boolean = false,
    val isDefinitionCheck: Boolean = false,
)

@Stable
data class WordState(
    val id: Long = NOT_IN_DB,
    val added: Date? = null,
    val value: String = "",
    val isEditMode: Boolean = false,
    val edited: String = "",
    val showWarningDialog: Boolean = false,
)

@Stable
data class LexemeState(
    val id: Long = NOT_IN_DB,
    val translation: TextValueState? = null,
    val definition: TextValueState? = null,
    val isMenuOpen: Boolean = false,
)

@Stable
data class TextValueState(
    val isEdit: Boolean = true,
    val origin: String = "",
    val edited: String = origin,
)

fun TextValueState.toValue(isEdit: Boolean) = if (isEdit) edited else origin
fun TextValueState.isChanged() = origin != edited

// TODO: вынести в mate
@Immutable
data class SnackbarState(
    val title: String = EMPTY_STRING,
    val show: Boolean = false,
)

/**
 * ###### LOADING STATE ######
 */
fun WordCardState.showLoading(): WordCardState =
    this.copy(isLoading = true)

fun WordCardState.hideLoading(): WordCardState =
    this.copy(isLoading = false)

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
 * ###### ADD LEXEME BOTTOM STATE ######
 */
fun WordCardState.showAddLexemeBottom(): WordCardState =
    this.copy(
        addLexemeBottomState = this.addLexemeBottomState.copy(
            show = true
        )
    )

fun WordCardState.hideAddLexemeBottom(): WordCardState =
    this.copy(
        addLexemeBottomState = this.addLexemeBottomState.copy(
            show = false
        )
    )

fun WordCardState.setTranslationCheck(checked: Boolean): WordCardState =
    this.copy(
        addLexemeBottomState = this.addLexemeBottomState.copy(
            isTranslationCheck = checked
        )
    )

fun WordCardState.setDefinitionCheck(checked: Boolean): WordCardState =
    this.copy(
        addLexemeBottomState = this.addLexemeBottomState.copy(
            isDefinitionCheck = checked
        )
    )

/**
 * ###### WORD STATE ######
 */
fun WordCardState.setWordId(id: Long): WordCardState =
    this.copy(
        wordState = this.wordState.copy(
            id = id
        )
    )

fun WordCardState.setWordAdded(date: Date): WordCardState =
    this.copy(
        wordState = this.wordState.copy(
            added = date
        )
    )

fun WordCardState.setWordValue(value: String): WordCardState =
    this.copy(
        wordState = this.wordState.copy(
            value = value
        )
    )

fun WordCardState.enableWordEdit(): WordCardState =
    this.copy(
        wordState = this.wordState.copy(
            isEditMode = true,
            edited = this.wordState.value
        )
    )

fun WordCardState.disableWordEdit(): WordCardState =
    this.copy(
        wordState = this.wordState.copy(
            isEditMode = false,
            edited = ""
        )
    )

fun WordCardState.updateWordEdited(edited: String): WordCardState =
    this.copy(
        wordState = this.wordState.copy(
            edited = edited
        )
    )

fun WordCardState.showWordWarningDialog(): WordCardState =
    this.copy(
        wordState = this.wordState.copy(
            showWarningDialog = true
        )
    )

fun WordCardState.hideWordWarningDialog(): WordCardState =
    this.copy(
        wordState = this.wordState.copy(
            showWarningDialog = false
        )
    )


fun WordCardState.setTerm(term: Term): WordCardState =
    this.copy(
        wordState = this.wordState.copy(
            id = term.wordId.id,
            value = term.word.value,
            added = term.addedDate
        )
    )

/**
 * ###### LEXEME LIST ######
 */
fun WordCardState.setLexemeList(lexemes: List<LexemeState>): WordCardState =
    this.copy(lexemeList = lexemes)

fun WordCardState.addLexeme(lexeme: LexemeState): WordCardState =
    this.copy(
        lexemeList = this.lexemeList + lexeme
    )

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

fun WordCardState.toggleLexemeMenu(lexemeId: Long): WordCardState =
    this.copy(
        lexemeList = this.lexemeList.map { lexeme ->
            if (lexeme.id == lexemeId) {
                lexeme.copy(isMenuOpen = !lexeme.isMenuOpen)
            } else {
                lexeme
            }
        }
    )

/**
 * ###### TEXT VALUE STATE ######
 */
fun TextValueState.setOrigin(text: String): TextValueState =
    this.copy(
        origin = text,
        edited = text
    )

fun TextValueState.setEdited(text: String): TextValueState =
    this.copy(edited = text)

fun TextValueState.enableEdit(): TextValueState =
    this.copy(isEdit = true)

fun TextValueState.disableEdit(): TextValueState =
    this.copy(isEdit = false)

fun TextValueState.resetToOrigin(): TextValueState =
    this.copy(edited = origin)

/**
 * ###### SNACKBAR STATE ######
 */
fun WordCardState.showSnackbar(title: String): WordCardState =
    this.copy(
        snackbarState = SnackbarState(
            title = title,
            show = true
        )
    )

fun WordCardState.hideSnackbar(): WordCardState =
    this.copy(
        snackbarState = SnackbarState(
            title = "",
            show = false
        )
    )

/**
 * ###### SCREEN NAVIGATION ######
 */
fun WordCardState.closeScreen(): WordCardState =
    this.copy(closeScreen = true)

/**
 * ###### ENTITY MAPPING ######
 */
fun Lexeme.toLexemeState(): LexemeState = LexemeState(
    id = this.lexemeId.id,
    translation = this.translation?.let { translation ->
        TextValueState(
            origin = translation.value,
            isEdit = false
        )
    },
    definition = this.definition?.let { definition ->
        TextValueState(
            origin = definition.value,
            isEdit = false
        )
    }
)

/**
 * ###### SPECIALIZED LEXEME EXTENSIONS ######
 */

// Lexeme menu management
fun WordCardState.setLexemeMenuOpen(lexemeId: Long, isOpen: Boolean): WordCardState =
    this.updateLexeme(lexemeId) { it.copy(isMenuOpen = isOpen) }

// Translation management
fun WordCardState.createLexemeTranslation(lexemeId: Long): WordCardState =
    this.updateLexeme(lexemeId) {
        it.copy(
            translation = TextValueState(
                origin = "",
                isEdit = true
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
            translation = it.translation?.copy(isEdit = true)
        )
    }

fun WordCardState.refreshLexemeTranslation(lexemeId: Long, newOrigin: String): WordCardState =
    this.updateLexeme(lexemeId) {
        it.copy(
            translation = it.translation?.copy(
                origin = newOrigin,
                isEdit = false
            )
        )
    }

// Definition management
fun WordCardState.createLexemeDefinition(lexemeId: Long): WordCardState =
    this.updateLexeme(lexemeId) {
        it.copy(
            definition = TextValueState(
                origin = "",
                isEdit = true
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
            definition = it.definition?.copy(isEdit = true)
        )
    }

fun WordCardState.refreshLexemeDefinition(lexemeId: Long, newOrigin: String): WordCardState =
    this.updateLexeme(lexemeId) {
        it.copy(
            definition = it.definition?.copy(
                origin = newOrigin,
                isEdit = false
            )
        )
    }