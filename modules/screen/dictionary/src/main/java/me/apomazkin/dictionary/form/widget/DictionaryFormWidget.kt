package me.apomazkin.dictionary.form.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.dictionary.R
import me.apomazkin.dictionary.form.DictionaryFormMsg
import me.apomazkin.dictionary.form.DictionaryFormScreenState
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.grayTextColor
import me.apomazkin.ui.ImageFlagWidget
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (formState.selectedFlag != null) {
                ImageFlagWidget(
                    flagRes = formState.selectedFlag.flagRes,
                    modifier = Modifier.size(48.dp),
                )
            } else {
                FlagPlaceholderWidget(
                    letter = formState.name.firstOrNull()?.toString() ?: "",
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            LexemeTextFieldWidget(
                modifier = Modifier.weight(1f),
                value = formState.name,
                onValueChange = { sendMsg(DictionaryFormMsg.NameChanged(it)) },
                placeHolder = R.string.dictionary_name_hint,
                onKeyboardActions = {},
            )
        }

        Spacer(modifier = Modifier.padding(top = 8.dp))

        OutlinedTextField(
            value = formState.flagFilter,
            onValueChange = { sendMsg(DictionaryFormMsg.FlagFilterChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LexemeStyle.BodyM,
            placeholder = {
                Text(
                    text = stringResource(id = R.string.dictionary_filter_flags_hint),
                    style = LexemeStyle.BodyM.copy(color = grayTextColor),
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (formState.flagFilter.isNotEmpty()) {
                    IconButton(
                        onClick = { sendMsg(DictionaryFormMsg.FlagFilterChanged("")) },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                        )
                    }
                }
            },
        )

        Spacer(modifier = Modifier.padding(top = 8.dp))

        FlagGridWidget(
            flags = formState.flags,
            selectedFlag = formState.selectedFlag,
            onFlagClick = { sendMsg(DictionaryFormMsg.SelectFlag(it)) },
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.padding(top = 8.dp))

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

        Spacer(modifier = Modifier.height(16.dp))
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
