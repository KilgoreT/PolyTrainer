package me.apomazkin.wordcard.widget

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.unselectedGreyColor
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.IconBoxed
import me.apomazkin.ui.R
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.ui.text.PrimaryEditableWidget
import me.apomazkin.wordcard.mate.TextValueState

@Composable
fun LexemeValueFieldWidget(
    state: TextValueState,
    @StringRes titleRes: Int,
    onTextChange: (String) -> Unit,
    onOpenEditMode: () -> Unit,
    onCloseEditMode: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PrimaryEditableWidget(
            modifier = Modifier
                .weight(1f),
            titleRes = titleRes,
            isEditMode = state.isEdit,
            originValue = state.origin,
            changedValue = state.edited,
            onTextChange = onTextChange,
            onOpenEditMode = onOpenEditMode,
            onCloseEditMode = onCloseEditMode,
        )
        IconBoxed(
            modifier = Modifier,
            iconRes = R.drawable.ic_circle_delete,
            size = 32,
            enabled = true,
            colorEnabled = unselectedGreyColor,
        )
    }
}

@PreviewWidget
@Composable
private fun Preview(
    @PreviewParameter(BoolParam::class) isEditMode: Boolean
) {
    AppTheme {
        Box(
            modifier = Modifier
                .background(whiteColor)
                .padding(24.dp),
        ) {
            LexemeValueFieldWidget(
                state = TextValueState(
                    origin = "origin origin origin qwe qwe qwe qw eqw e qw e qwe",
                    edited = "a unit of meaning in a language",
                    isEdit = isEditMode,
                ),
                titleRes = R.string.word_card_bottom_translation,
                onTextChange = {},
                onOpenEditMode = {},
                onCloseEditMode = {},
            )
        }
    }
}