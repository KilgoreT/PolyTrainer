package me.apomazkin.quiztab.widget

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.quiztab.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.grayTextColor
import me.apomazkin.ui.preview.PreviewWidget

private const val defaultCornerRadius = 16
private const val defaultShadow = 4

@Composable
fun QuizItemWidget(
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int,
    @StringRes titleRes: Int,
    @StringRes subTitleRes: Int,
    cornerRadius: Int = defaultCornerRadius,
    shadow: Int = defaultShadow,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(cornerRadius.dp),
        shadowElevation = shadow.dp
    ) {
        Row(
            modifier = modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier,
                painter = painterResource(imageRes),
                contentDescription = stringResource(titleRes)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(titleRes),
                    style = LexemeStyle.BodyLBold.copy(
                        color = MaterialTheme.colorScheme.secondary,
                    )
                )
                Text(
                    text = stringResource(subTitleRes),
                    style = LexemeStyle.BodyL.copy(
                        color = grayTextColor,
                    )
                )
            }
        }
    }
}

@PreviewWidget
@Composable
private fun Preview() = AppTheme {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        QuizItemWidget(
            imageRes = R.drawable.ic_quiz_write,
            titleRes = R.string.quiz_item_title_write,
            subTitleRes = R.string.quiz_item_subtitle_write
        ) {}
    }
}