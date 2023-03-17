@file:OptIn(
    ExperimentalMaterial3Api::class,
)

package me.apomazkin.wordcard.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidgetEn

@Composable
fun EditableText(
    title: String,
    isEdit: Boolean,
    value: String,
    onTextChange: (String) -> Unit,
    titleStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    clearIcon: ImageVector = Icons.Default.Clear,
    clearIconContentDescription: String = stringResource(id = R.string.content_description_clear),
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = titleStyle,
            )
        }
        if (isEdit) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = value,
                onValueChange = onTextChange,
                trailingIcon = {
                    Icon(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onTextChange("") }
                            .padding(8.dp),
                        imageVector = clearIcon,
                        contentDescription = clearIconContentDescription
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    cursorColor = MaterialTheme.colorScheme.secondary,
                )
            )
        } else {
            Text(text = value)
        }
    }
}

@PreviewWidgetEn
@Composable
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            EditableText(
                title = "Definition",
                isEdit = false,
                value = "a unit of meaning in a language, consisting of a word or group of words",
                onTextChange = {},
            )
        }
    }
}

@PreviewWidgetEn
@Composable
private fun PreviewEditable() {
    AppTheme {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            EditableText(
                title = "Definition",
                isEdit = true,
                value = "a unit of meaning in a language, consisting of a word or group of words",
                onTextChange = {},
            )
        }
    }
}