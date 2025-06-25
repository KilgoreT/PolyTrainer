package me.apomazkin.quiz.chat.widget.appbar.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import me.apomazkin.icondropdowned.MenuItem
import me.apomazkin.icondropdowned.StringSource
import me.apomazkin.quiz.chat.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun EarliestReviewedMenuItem(
        isChecked: Boolean,
        onClick: (isChecked: Boolean) -> Unit,
) {
    MenuItem
            .withCheckbox(
                    isChecked = isChecked,
                    title = StringSource.fromRes(
                            resId = R.string.chat_menu_item_earliest_reviewed,
                            style = LexemeStyle.BodyL,
                    ),
                    onCheckedChange = onClick,
            )
            .Widget()
}

@PreviewWidget
@Composable
private fun Preview(
        @PreviewParameter(BoolParam::class) enabled: Boolean,
) {
    AppTheme {
        EarliestReviewedMenuItem(
                isChecked = enabled,
                onClick = {}
        )
    }
}