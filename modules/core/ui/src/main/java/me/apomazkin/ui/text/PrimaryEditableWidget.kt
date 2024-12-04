package me.apomazkin.ui.text

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.grayTextColor
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.R
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.ui.text.base.LexemeEditableText

@Composable
fun PrimaryEditableWidget(
    @StringRes titleRes: Int,
    isEditMode: Boolean,
    originValue: String,
    changedValue: String,
    onTextChange: (String) -> Unit,
    onOpenEditMode: () -> Unit,
    onCloseEditMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
    ) {
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!isEditMode) {
                Text(
                    modifier = Modifier,
                    text = stringResource(id = titleRes),
                    style = LexemeStyle.BodyS,
                    color = grayTextColor,
                )
            }
            LexemeEditableText(
                originValue = originValue,
                changedValue = changedValue,
                isEditMode = isEditMode,
                onTextChange = onTextChange,
                onOpenEditMode = onOpenEditMode,
                onCloseEditMode = onCloseEditMode,
                textColor = MaterialTheme.colorScheme.secondary,
            )
        }
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
            PrimaryEditableWidget(
                titleRes = R.string.word_card_bottom_translation,
                isEditMode = isEditMode,
                originValue = "the round fruit of a tree of the rose family, which typically has thin red or green skin and crisp flesh. Many varieties have been developed as dessert or cooking fruit or for making cider",
                changedValue = "111the round fruit of a tree of the rose family, which typically has thin red or green skin and crisp flesh. Many varieties have been developed as dessert or cooking fruit or for making cider",
                onTextChange = {},
                onOpenEditMode = {},
                onCloseEditMode = {},
            )
        }
    }
}