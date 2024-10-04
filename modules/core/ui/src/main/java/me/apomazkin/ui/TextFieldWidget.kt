@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package me.apomazkin.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.LexemeStyle

@Composable
fun TextFieldWidget(
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Default,
    erasable: Boolean = false,
    value: String,
    onValueChange: (String) -> Unit,
    onKeyboardActions: () -> Unit,
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    LaunchedEffect(value) {
        textFieldValue = textFieldValue.copy(
            text = value,
            selection = TextRange(value.length)
        )
    }

    OutlinedTextField(
        modifier = modifier,
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            onValueChange.invoke(it.text)
        },
        trailingIcon = {
            if (erasable && value.isNotBlank()) {
                Icon(
                    modifier = Modifier
                        .clickable { onValueChange.invoke("") },
                    painter = painterResource(id = R.drawable.ic_clear),
                    contentDescription = ""
                )
            }
        },
        singleLine = false,
        textStyle = LexemeStyle.BodyL,
        shape = RoundedCornerShape(0.dp),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = imeAction,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
        ),
        keyboardActions = KeyboardActions { onKeyboardActions.invoke() }
    )
}