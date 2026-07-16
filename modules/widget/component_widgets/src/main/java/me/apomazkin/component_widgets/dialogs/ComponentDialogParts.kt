package me.apomazkin.component_widgets.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.component_widgets.widgets.labelRes
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.destructiveRed
import me.apomazkin.theme.dialogFieldBg
import me.apomazkin.theme.radioBorderInactive
import me.apomazkin.theme.radioSelectedBg
import me.apomazkin.theme.templateChipBg
import me.apomazkin.theme.templateChipText
import me.apomazkin.theme.whiteColor

/** Caps-лейбл секции диалога компонента (Figma 5027:1599). */
@Composable
internal fun ComponentDialogLabel(@StringRes textRes: Int, accent: Boolean) {
    Text(
        text = stringResource(id = textRes),
        style = LexemeStyle.BodySBold.copy(letterSpacing = 0.36.sp),
        color = if (accent) LexemeColor.primary else templateChipText,
    )
}

/** Поле ввода имени компонента (Figma 5027:1600): фон dialogFieldBg, бордер primary, r13, курсор primary. */
@Composable
internal fun ComponentNameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(13.dp),
        color = dialogFieldBg,
        border = BorderStroke(1.dp, LexemeColor.primary),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 15.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LexemeStyle.BodyL.copy(color = LexemeColor.secondary),
                cursorBrush = SolidColor(LexemeColor.primary),
            )
        }
    }
}

/** Радио-ряд типа значения (Figma 5027:1604/1608). [enabled]=false — приглушён и не кликается. */
@Composable
internal fun ComponentRadioRow(
    @StringRes textRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = if (selected) radioSelectedBg else whiteColor,
        border = BorderStroke(1.dp, if (selected) LexemeColor.primary else radioBorderInactive),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(
                        width = 2.dp,
                        color = if (selected) LexemeColor.primary else radioBorderInactive,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color = LexemeColor.primary, shape = RoundedCornerShape(5.dp)),
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(id = textRes),
                style = if (selected) LexemeStyle.BodyMBold else LexemeStyle.BodyM,
                color = if (selected) LexemeColor.secondary else templateChipText,
            )
        }
    }
}

/** Чекбокс «Несколько значений» (Figma 5027:1611): квадрат primary r7 с галочкой. */
@Composable
internal fun ComponentMultiToggle(
    @StringRes textRes: Int,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = if (checked) LexemeColor.primary else whiteColor,
                    shape = RoundedCornerShape(7.dp),
                )
                .border(
                    width = if (checked) 0.dp else 1.dp,
                    color = radioBorderInactive,
                    shape = RoundedCornerShape(7.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    modifier = Modifier.size(15.dp),
                    painter = painterResource(id = R.drawable.ic_confirm),
                    contentDescription = null,
                    tint = whiteColor,
                )
            }
        }
        Text(
            text = stringResource(id = textRes),
            style = LexemeStyle.BodyM,
            color = LexemeColor.secondary,
        )
    }
}

/**
 * Кнопки диалога компонента (Figma 5027:1615/1617): «Отменить» серая + submit с тенью.
 * [destructive] окрашивает submit в [destructiveRed] (диалог удаления, без макета — тот же облик).
 */
@Composable
internal fun ComponentDialogActions(
    @StringRes submitRes: Int,
    submitEnabled: Boolean,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    destructive: Boolean = false,
) {
    val accent = if (destructive) destructiveRed else LexemeColor.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clickable(onClick = onCancel),
            shape = RoundedCornerShape(14.dp),
            color = templateChipBg,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(id = R.string.button_cancel),
                    style = LexemeStyle.BodyMBold,
                    color = templateChipText,
                    textAlign = TextAlign.Center,
                )
            }
        }
        val submitShape = RoundedCornerShape(14.dp)
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .let {
                    if (submitEnabled) it.shadow(
                        elevation = 8.dp,
                        shape = submitShape,
                        ambientColor = accent.copy(alpha = 0.5f),
                        spotColor = accent.copy(alpha = 0.5f),
                    ) else it
                }
                .clickable(enabled = submitEnabled, onClick = onSubmit),
            shape = submitShape,
            color = if (submitEnabled) accent else radioBorderInactive,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(id = submitRes),
                    style = LexemeStyle.BodyMBold,
                    color = whiteColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * TODO(IS485): временно недоступные для выбора шаблоны — фича картинок ещё не готова.
 * Снять дизейбл = убрать значение из набора.
 */
private val DISABLED_TEMPLATES = setOf(ComponentTemplate.IMAGE)

/** Ряды radio для типа значения (общий для Create/Edit). */
@Composable
internal fun ComponentTemplateRadioGroup(
    selected: ComponentTemplate,
    onSelect: (ComponentTemplate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ComponentTemplate.entries.forEach { t ->
            ComponentRadioRow(
                textRes = t.labelRes(),
                selected = t == selected,
                onClick = { onSelect(t) },
                enabled = t !in DISABLED_TEMPLATES,
            )
        }
    }
}
