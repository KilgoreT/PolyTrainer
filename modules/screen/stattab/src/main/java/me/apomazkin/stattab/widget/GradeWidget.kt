package me.apomazkin.stattab.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.dividerColor
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun GradeWidget(
    value: String,
    grade: String,
    bgColor: Color,
    fgColor: Color,
) {
    Surface(
        modifier = Modifier,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
        border = BorderStroke(width = 1.dp, color = dividerColor)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = LexemeStyle.H5
                    .copy(color = MaterialTheme.colorScheme.secondary),
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(bgColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(fgColor)
                )
                Text(
                    text = grade,
                    style = LexemeStyle.BodyS.copy(
                        color = fgColor,
                    )
                )
            }
        }
    }
}


@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .padding(20.dp)
        ) {
            GradeWidget(
                value = "76",
                grade = "В процессе",
                fgColor = Color(0xFF3AA981),
                bgColor = Color(0xFFDAF1E4)
            )
        }
    }
}