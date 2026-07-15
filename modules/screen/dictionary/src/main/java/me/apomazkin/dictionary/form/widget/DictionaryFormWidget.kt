package me.apomazkin.dictionary.form.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.dictionary.R
import me.apomazkin.dictionary.form.DictionaryFormMsg
import me.apomazkin.dictionary.form.DictionaryFormScreenState
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun DictionaryFormWidget(
    formState: DictionaryFormScreenState,
    showTitle: Boolean,
    sendMsg: (DictionaryFormMsg) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        FormHeaderWidget(
            showTitle = showTitle,
            modifier = Modifier.padding(top = 12.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlagPreviewWidget(
                selectedFlag = formState.selectedFlag,
                name = formState.name,
            )
            Spacer(modifier = Modifier.width(14.dp))
            NameFieldWidget(
                value = formState.name,
                onValueChange = { sendMsg(DictionaryFormMsg.NameChanged(it)) },
                labelRes = R.string.dictionary_name_hint,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SearchPillWidget(
            value = formState.flagFilter,
            onValueChange = { sendMsg(DictionaryFormMsg.FlagFilterChanged(it)) },
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlagGridWidget(
            flags = formState.flags,
            selectedFlag = formState.selectedFlag,
            isFilterActive = formState.flagFilter.isNotBlank(),
            onFlagClick = { sendMsg(DictionaryFormMsg.SelectFlag(it)) },
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.height(8.dp))

        val buttonTextRes = if (formState.editingDictionaryId != null) {
            R.string.dictionary_save
        } else {
            R.string.dictionary_create
        }

        SubmitButtonWidget(
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
            showTitle = true,
            sendMsg = {},
        )
    }
}
