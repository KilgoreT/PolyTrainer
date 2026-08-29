@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.ui.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.input.PrimaryTextFieldWidget
import me.apomazkin.ui.preview.PreviewWidget

/**
 * IS493 Э3: общая шторка «поле ввода + отправка» — единая вёрстка для
 * добавления слова (words) и создания/переименования группы (groups);
 * вынесена по решению юзера (ручной прогон Э3).
 *
 * [errorText] — опциональная строка ошибки валидации ПОД полем: снекбар
 * host'а под открытой M3-шторкой (отдельное окно) и клавиатурой не виден —
 * ошибка показывается на месте ввода.
 */
@Composable
fun InputBottomSheetWidget(
    value: String,
    isSendEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onSendAction: () -> Unit,
    onDismissRequest: () -> Unit,
    errorText: String? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = Modifier,
        containerColor = MaterialTheme.colorScheme.onPrimary,
        dragHandle = {},
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
    ) {
        Column {
            PrimaryTextFieldWidget(
                modifier = Modifier,
                isSendEnabled = isSendEnabled,
                value = value,
                onValueChange = onValueChange,
                onSendAction = onSendAction,
            )
            if (errorText != null) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    text = errorText,
                    style = LexemeStyle.BodyM,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewWithError() {
    AppTheme {
        InputBottomSheetWidget(
            value = "Дом",
            isSendEnabled = true,
            onValueChange = {},
            onSendAction = {},
            onDismissRequest = {},
            errorText = "Имя уже занято",
        )
    }
}
