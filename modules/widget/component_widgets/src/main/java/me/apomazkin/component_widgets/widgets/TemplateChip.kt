package me.apomazkin.component_widgets.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.templateChipBg
import me.apomazkin.theme.templateChipText

/** Нейтральный чип типа значения компонента (Figma 5027:1560): плашка + иконка + текст. */
@Composable
internal fun TemplateChip(
    template: ComponentTemplate,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = templateChipBg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                modifier = Modifier.size(13.dp),
                painter = painterResource(id = R.drawable.ic_text_lines),
                contentDescription = null,
                tint = templateChipText,
            )
            Text(
                text = stringResource(id = template.labelRes()),
                style = LexemeStyle.BodyS,
                color = templateChipText,
            )
        }
    }
}
