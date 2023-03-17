@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.vocabulary.ui.widget.detailDialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.vocabulary.logic.EditableTextState

@Composable
fun WordDetailTextField(
    state: EditableTextState,
    title: String,
    onEditStart: () -> Unit,
    onTextChange: (String) -> Unit,
    onEditFinish: (String) -> Unit,
    titleStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    clearIcon: ImageVector = Icons.Default.Clear,
    clearIconContentDescription: String = stringResource(id = R.string.content_description_clear),
) {
    val editIcon = remember(state.readOnly) {
        derivedStateOf { if (state.readOnly) Icons.Default.Edit else Icons.Default.Done }
    }

    val text = remember(state.readOnly) {
        derivedStateOf { if (state.readOnly) state.text else state.editedText }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = titleStyle,
            )
            Spacer(modifier = Modifier.weight(1F))
            Icon(
                modifier = Modifier
                    .clickable {
                        if (state.readOnly) onEditStart()
                        else onEditFinish(text.value)
                    },
                imageVector = editIcon.value,
                contentDescription = ""
            )
        }
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = text.value,
            readOnly = state.readOnly,
            onValueChange = onTextChange,
            trailingIcon = {
                if (!state.readOnly) {
                    Icon(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onTextChange("") }
                            .padding(8.dp),
                        imageVector = clearIcon,
                        contentDescription = clearIconContentDescription
                    )
                }
            },
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                cursorColor = MaterialTheme.colorScheme.secondary,
            )
        )
    }
}