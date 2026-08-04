package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.PopupProperties
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formTextSecondary

/**
 * IS491: caption-поле captioned_text-значения — combobox «одно поле, две роли»
 * (Д4/UC4): ввод нового значения И фильтр выпадающего списка подсказок.
 * Выбор пункта подставляет значение; фильтрация локальная (без запросов);
 * список скрыт при отсутствии совпадений и после выбора.
 *
 * @param onFocused фокус в поле — родитель триггерит one-shot загрузку подсказок.
 * @param onFocusLost потеря фокуса — родитель коммитит edit.
 */
@Composable
internal fun CaptionField(
    caption: String,
    suggestions: List<String>,
    enabled: Boolean,
    onCaptionChange: (String) -> Unit,
    onFocused: () -> Unit,
    onFocusLost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }
    // Список подавлен после выбора пункта — до следующего изменения текста.
    var suppressed by remember { mutableStateOf(false) }

    val filtered = suggestions.filter { it.contains(caption, ignoreCase = true) }
    val expanded = focused && !suppressed && filtered.isNotEmpty()

    Box(modifier = modifier) {
        BasicTextField(
            modifier = Modifier.onFocusChanged { focusState ->
                focused = focusState.isFocused
                if (focusState.isFocused) {
                    hadFocus = true
                    // Каждый заход в поле снова показывает список (сбрасываем
                    // подавление, выставленное при dismiss/уходе фокуса).
                    suppressed = false
                    onFocused()
                } else if (hadFocus) {
                    onFocusLost()
                }
            },
            value = caption,
            onValueChange = {
                suppressed = false
                onCaptionChange(it)
            },
            enabled = enabled,
            singleLine = true,
            textStyle = LexemeStyle.BodyM.copy(color = MaterialTheme.colorScheme.secondary),
            decorationBox = { innerTextField ->
                if (caption.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.word_card_caption_hint),
                        style = LexemeStyle.BodyM,
                        color = formTextSecondary,
                    )
                }
                innerTextField()
            },
        )
        DropdownMenu(
            // Явный светлый фон — дефолтный контейнер меню в теме тёмный (текст не виден).
            modifier = Modifier.background(color = MaterialTheme.colorScheme.surface),
            expanded = expanded,
            onDismissRequest = { suppressed = true },
            // Не красть фокус у поля ввода — иначе клавиатура закрывается.
            properties = PopupProperties(focusable = false),
        ) {
            filtered.forEach { suggestion ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = suggestion,
                            style = LexemeStyle.BodyM,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    },
                    onClick = {
                        suppressed = true
                        onCaptionChange(suggestion)
                    },
                )
            }
        }
    }
}
