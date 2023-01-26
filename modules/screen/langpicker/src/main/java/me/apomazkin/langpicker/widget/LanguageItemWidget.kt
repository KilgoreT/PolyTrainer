package me.apomazkin.langpicker.widget

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blongho.country_data.R
import me.apomazkin.theme.AppTheme

@Composable
fun LanguageItemWidget(
    @DrawableRes flagRes: Int,
    value: String,
    isSelected: Boolean,
    onClick: (value: String) -> Unit,
) {
    val color = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color)
            .background(Color.Transparent)
            .clickable { onClick.invoke(value) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(24.dp)
                .border(
                    width = 1.dp,
                    color = Color.Black,
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp)),
            painter = painterResource(id = flagRes),
            contentScale = ContentScale.FillHeight,
            contentDescription = value
        )
        Text(
            modifier = Modifier
                .padding(end = 16.dp),
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewNotSelect() {
    AppTheme {
        LanguageItemWidget(
            flagRes = R.drawable.gb,
            value = "English",
            isSelected = false
        ) {}
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewSelected() {
    AppTheme {
        LanguageItemWidget(
            flagRes = R.drawable.gb,
            value = "English",
            isSelected = true
        ) {}
    }
}