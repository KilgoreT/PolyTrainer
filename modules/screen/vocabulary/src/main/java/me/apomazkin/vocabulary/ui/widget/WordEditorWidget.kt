@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.vocabulary.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.clr1F001F24
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun WordEditorWidget(
    label: String,
    wordValue: String,
    onWordValueChange: (String) -> Unit,
    onAddWord: (String) -> Unit,
    enabledColor: Color = MaterialTheme.colorScheme.primary,
    disabledColor: Color = clr1F001F24
) {
    val isTextTyped by remember(wordValue) { mutableStateOf(wordValue.isNotEmpty()) }
    val iconColor by remember(isTextTyped) {
        derivedStateOf {
            if (isTextTyped) {
                enabledColor
            } else {
                disabledColor
            }
        }
    }
    TextField(
        modifier = Modifier
            .fillMaxWidth(),
        value = wordValue,
        onValueChange = onWordValueChange,
        trailingIcon = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color = iconColor)
                    .clickable(isTextTyped) { onAddWord(wordValue) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    contentDescription = ""
                )
            }
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { if (isTextTyped) onAddWord(wordValue) }
        ),
//        colors = TextFieldDefaults.textFieldColors(
//            containerColor = MaterialTheme.colorScheme.background,
//            focusedIndicatorColor = MaterialTheme.colorScheme.primary
//        )
    )
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        WordEditorWidget(
            label = "label",
            wordValue = "",
            onWordValueChange = {},
            onAddWord = {},
        )
    }
}