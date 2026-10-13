package me.apomazkin.wordstab.ui.widget.photo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.cardDivider
import me.apomazkin.theme.destructiveRed
import me.apomazkin.theme.formBackground
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.theme.formTextTertiary
import me.apomazkin.ui.btn.AlarmButtonWidget
import me.apomazkin.ui.btn.CancelButtonWidget
import me.apomazkin.ui.btn.PrimaryFullButtonWidget
import me.apomazkin.ui.dialog.base.LexemeDialog
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.logic.Msg
import me.apomazkin.wordstab.logic.PhotoWordsState
import me.apomazkin.wordstab.logic.photo.PhotoNotAdded

/** Страница лицензии данных источника (WFT-57.2). */
private const val LICENSE_URL = "https://creativecommons.org/licenses/by-sa/4.0/"

/**
 * IS532: окно добавления на весь экран — черновик (AWP-03). Диалог без
 * ширины платформы и без закрытия тапом снаружи (AWP-14); системная
 * «назад» — [Msg.RequestClosePhotoWords] (AWP-19). Пока идёт запись,
 * окно не принимает нажатий (AWP-13.4). Цвета явные, парой из
 * PhotoWordsDialog: фон formBackground, текст formTextTertiary, второй
 * план formTextSecondary, ошибка destructiveRed.
 */
@Composable
internal fun PhotoAddWindow(
    state: PhotoWordsState,
    sendMessage: (Msg) -> Unit,
) {
    Dialog(
        onDismissRequest = { sendMessage(Msg.RequestClosePhotoWords) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = true,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            color = formBackground,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
                    WindowTopBar(onClose = { sendMessage(Msg.RequestClosePhotoWords) })
                    WindowList(
                        state = state,
                        sendMessage = sendMessage,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.cards.isNotEmpty()) {
                        PhotoBottomPanel(state = state, sendMessage = sendMessage)
                    }
                }
                if (state.isAdding) {
                    // Нажатия не доходят до окна, пока летит запись (AWP-02, AWP-13.4).
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                    )
                }
            }
        }
        PhotoSheets(state = state, sendMessage = sendMessage)
        if (state.isConfirmingAdd) {
            AddConfirmDialog(state = state, sendMessage = sendMessage)
        }
        if (state.isConfirmingClose) {
            CloseConfirmDialog(sendMessage = sendMessage)
        }
    }
}

@Composable
private fun WindowTopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "×",
            style = LexemeStyle.H5,
            color = formTextTertiary,
            modifier = Modifier
                .clickable { onClose() }
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
        Text(
            text = stringResource(id = R.string.photo_words_title),
            style = LexemeStyle.BodyXLBold,
            color = formTextTertiary,
        )
    }
    HorizontalDivider(color = cardDivider)
}

/** Шапка, карточки, «Добавить нечего», блок «не добавить», атрибуция — одним списком (AWP-04.6). */
@Composable
private fun WindowList(
    state: PhotoWordsState,
    sendMessage: (Msg) -> Unit,
    modifier: Modifier,
) {
    val form = state.form ?: return
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        if (state.cards.isNotEmpty()) {
            item(key = "header") {
                PhotoHeaderWidget(state = state, form = form, sendMessage = sendMessage)
                HorizontalDivider(color = cardDivider)
            }
        } else {
            item(key = "nothing") {
                Text(
                    text = stringResource(id = R.string.photo_add_nothing),
                    style = LexemeStyle.BodyM,
                    color = formTextTertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                )
                HorizontalDivider(color = cardDivider)
            }
        }
        items(state.cards, key = { it.lemma }) { card ->
            PhotoCardWidget(card = card, state = state, form = form, sendMessage = sendMessage)
            HorizontalDivider(color = cardDivider)
        }
        if (state.notAdded.isNotEmpty()) {
            item(key = "not-added") {
                NotAddedBlock(items = state.notAdded, isExpanded = state.isNotAddedExpanded, sendMessage = sendMessage)
                HorizontalDivider(color = cardDivider)
            }
        }
        item(key = "attribution") {
            Attribution()
        }
    }
}

/** Слова без добавления (AWP-12): свёрнутый блок в конце, развёрнут, когда карточек нет. */
@Composable
private fun NotAddedBlock(
    items: List<PhotoNotAdded>,
    isExpanded: Boolean,
    sendMessage: (Msg) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { sendMessage(Msg.TogglePhotoNotAdded) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.photo_add_not_added, items.size),
                style = LexemeStyle.BodyM,
                color = formTextTertiary,
            )
            Text(
                text = if (isExpanded) "▴" else "▾",
                style = LexemeStyle.BodyM,
                color = formTextSecondary,
            )
        }
        if (isExpanded) {
            items.forEach { item ->
                Text(
                    text = "${item.text} — ${notAddedReasonLabel(item.reason)}",
                    style = LexemeStyle.BodyS,
                    color = formTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** «Данные: Wiktionary, CC BY-SA 4.0», нажатие открывает лицензию (WFT-57). */
@Composable
private fun Attribution() {
    val uriHandler = LocalUriHandler.current
    Text(
        text = stringResource(id = R.string.photo_words_attribution),
        style = LexemeStyle.BodyS,
        color = formTextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(LICENSE_URL) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/**
 * Панель внизу (AWP-13, набросок §9): кнопка со счётом; причина
 * неактивной кнопки и «N уже есть» над ней; «не добавилось» с
 * «повторить» (В3, AWP-26).
 */
@Composable
private fun PhotoBottomPanel(
    state: PhotoWordsState,
    sendMessage: (Msg) -> Unit,
) {
    val summary = state.summary
    val failure = state.addFailure
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(formBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        HorizontalDivider(color = cardDivider)
        Spacer(modifier = Modifier.height(8.dp))
        if (summary.existingCount > 0) {
            PanelNote(text = pluralStringResource(id = R.plurals.photo_add_existing_skipped, count = summary.existingCount, summary.existingCount))
        }
        when {
            failure != null -> PanelNote(text = failureLabel(failure), isError = true)
            state.isAdding -> Unit
            summary.noCoreMapping -> PanelNote(text = stringResource(id = R.string.photo_add_mapping_no_core), isError = true)
            summary.lacksCoreCount > 0 -> PanelNote(
                text = pluralStringResource(id = R.plurals.photo_add_no_core_reason, count = summary.lacksCoreCount, summary.lacksCoreCount),
                isError = true,
            )
            summary.lexemeCount == 0 -> PanelNote(text = stringResource(id = R.string.photo_add_nothing_to_add))
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (failure != null) {
            PrimaryFullButtonWidget(
                modifier = Modifier.fillMaxWidth(),
                titleRes = R.string.photo_add_retry,
                enabled = summary.isEnabled && !state.isAdding,
                onClick = { sendMessage(Msg.RetryPhotoAdd) },
            )
        } else {
            AddButton(state = state, sendMessage = sendMessage)
        }
    }
}

@Composable
private fun PanelNote(
    text: String,
    isError: Boolean = false,
) {
    Text(
        text = text,
        style = LexemeStyle.BodyS,
        color = if (isError) destructiveRed else formTextSecondary,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

/** Текст кнопки — ресурс не умеет счёт, поэтому кнопка собирается из общей кнопки с подменой текста через ширину. */
@Composable
private fun AddButton(
    state: PhotoWordsState,
    sendMessage: (Msg) -> Unit,
) {
    val summary = state.summary
    val title = when {
        state.isAdding -> stringResource(id = R.string.photo_add_adding)
        summary.isEnabled -> stringResource(id = R.string.photo_add_button, countInWords(summary.lexemeCount, summary.wordCount))
        else -> stringResource(id = R.string.photo_add_button_plain)
    }
    AlarmButtonWidget(
        modifier = Modifier.fillMaxWidth(),
        titleRes = R.string.photo_add_button_plain,
        titleOverride = title,
        enabled = summary.isEnabled && !state.isAdding,
        containerColor = me.apomazkin.theme.LexemeColor.primary,
        contentColor = me.apomazkin.theme.LexemeColor.onPrimary,
    ) { sendMessage(Msg.RequestPhotoAdd) }
}

/** «Добавить N значений в M слов?» — всегда перед записью (AWP-13, Д23). */
@Composable
private fun AddConfirmDialog(
    state: PhotoWordsState,
    sendMessage: (Msg) -> Unit,
) {
    LexemeDialog(onDismissRequest = { sendMessage(Msg.CancelPhotoAdd) }) {
        Text(
            text = stringResource(id = R.string.photo_add_confirm, countInWords(state.summary.lexemeCount, state.summary.wordCount)),
            style = LexemeStyle.H6,
            color = formTextTertiary,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CancelButtonWidget(modifier = Modifier.weight(1f)) { sendMessage(Msg.CancelPhotoAdd) }
            PrimaryFullButtonWidget(
                modifier = Modifier.weight(1f),
                titleRes = R.string.photo_add_yes,
                onClick = { sendMessage(Msg.ConfirmPhotoAdd) },
            )
        }
    }
}

/** «Закрыть без добавления?» после удалений и правок (AWP-25). */
@Composable
private fun CloseConfirmDialog(sendMessage: (Msg) -> Unit) {
    LexemeDialog(onDismissRequest = { sendMessage(Msg.StayInPhotoWords) }) {
        Text(
            text = stringResource(id = R.string.photo_add_close_question),
            style = LexemeStyle.H6,
            color = formTextTertiary,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CancelButtonWidget(
                modifier = Modifier.weight(1f),
                titleRes = R.string.photo_add_stay,
            ) { sendMessage(Msg.StayInPhotoWords) }
            AlarmButtonWidget(
                modifier = Modifier.weight(1f),
                titleRes = R.string.photo_add_close,
            ) { sendMessage(Msg.ConfirmClosePhotoWords) }
        }
    }
}
