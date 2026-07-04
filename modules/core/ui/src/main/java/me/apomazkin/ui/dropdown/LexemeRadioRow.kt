package me.apomazkin.ui.dropdown

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * IS481 (F162): primitive — radio-row для radio-group в формах/диалогах.
 *
 * Отличается от [LexemeRadioMenuItem] (DropdownMenuItem) тем, что используется как
 * inline-row в Column/LazyColumn, а не как пункт меню. Поддерживает full-row click area
 * через `Modifier.selectable(... role = Role.RadioButton)`.
 *
 * @param textRes label string resource.
 * @param selected текущий radio-state.
 * @param onClick клик-callback (caller — single-selection radio-group).
 * @param modifier дополнительные modifier'ы (caller).
 * @param color цвет текста; `Color.Unspecified` (default) → текст наследует
 *   `LocalContentColor`. Caller может передать explicit для contrast-fix.
 */
@Composable
fun LexemeRadioRow(
    @StringRes textRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(id = textRes),
            color = color,
        )
    }
}
