package me.apomazkin.dictionary.form.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.dictionary.R
import me.apomazkin.dictionary.form.DictionaryFormMsg
import me.apomazkin.dictionary.form.DictionaryFormScreenState
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.grayTextColor
import me.apomazkin.ui.btn.PrimaryFullButtonWidget
import me.apomazkin.ui.input.base.LexemeTextFieldWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun DictionaryFormWidget(
    formState: DictionaryFormScreenState,
    sendMsg: (DictionaryFormMsg) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        LexemeTextFieldWidget(
            modifier = Modifier.fillMaxWidth(),
            value = formState.name,
            onValueChange = { sendMsg(DictionaryFormMsg.NameChanged(it)) },
            placeHolder = R.string.dictionary_name_hint,
            onKeyboardActions = {},
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { sendMsg(DictionaryFormMsg.ToggleLanguageBound) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = formState.isLanguageBound,
                onCheckedChange = { sendMsg(DictionaryFormMsg.ToggleLanguageBound) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.dictionary_bind_language),
                style = LexemeStyle.BodyM,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        if (formState.isLanguageBound) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { sendMsg(DictionaryFormMsg.OpenLanguagePicker) },
            ) {
                OutlinedTextField(
                    value = formState.selectedLanguage?.displayName ?: "",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    placeholder = {
                        Text(
                            text = stringResource(
                                id = R.string.dictionary_bind_language
                            ),
                            style = LexemeStyle.BodyM.copy(color = grayTextColor),
                        )
                    },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_next),
                            contentDescription = null,
                        )
                    },
                    textStyle = LexemeStyle.BodyL,
                )
            }

            if (formState.selectedLanguage != null && formState.availableFlags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                FlagGridWidget(
                    flags = formState.availableFlags,
                    selectedFlag = formState.selectedFlag,
                    onFlagClick = { sendMsg(DictionaryFormMsg.SelectFlag(it)) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val buttonTextRes = if (formState.editingDictionaryId != null) {
            R.string.dictionary_save
        } else {
            R.string.dictionary_create
        }

        PrimaryFullButtonWidget(
            titleRes = buttonTextRes,
            enabled = formState.saveButtonEnabled,
            onClick = { sendMsg(DictionaryFormMsg.Save) },
        )
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        DictionaryFormWidget(
            formState = DictionaryFormScreenState(name = "Test"),
            sendMsg = {},
        )
    }
}
